package com.bbva.kyof.vega.sockets;

import com.bbva.kyof.vega.exception.LLZException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.nio.ByteBuffer;

/**
 * Wrapper to handle a worker responder socket in ZMQ. Responder worker sockets listen for request
 * coming from a dealer and are able to send responses.
 *
 * This class is thread-safe
 */
public final class LLZRespWorkerSocket implements Runnable
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZRespWorkerSocket.class);

    /** Worker protocol that will receive the requests and send the responses */
    private final ZMQ.Socket workerSocket;

    /** Handler for incoming requests */
    private final ILLZRespSocketReqHandler requestReceiver;

    /** Lock object for the class */
    private final Object lock = new Object();

    /** Name of the publisher the socket belongs to */
    private final String publisherName;

    /** True if the socket has already been stopped */
    private boolean stopped = false;

    /**
     * Create a new response worker given the context and the ID for inter protocol communication with the ZMQ dealer and ZMQ router
     *
     * @param context the context the protocol belongs to
     * @param publisherName name of the publisher the worker socket belongs to
     * @param inProcSocketId the unique ID for inter process communication
     * @param requestReceiver receiver for the incoming requests on the socket
     */
    public LLZRespWorkerSocket(final ZMQ.Context context, final String publisherName, final String inProcSocketId, final ILLZRespSocketReqHandler requestReceiver) throws LLZException
    {        
        LOGGER.debug("Creating ZMQ RESP WORKER socket for LLZ Publisher [{}] and interproc Id [{}]", publisherName, inProcSocketId);

        this.publisherName = publisherName;

        try
        {
            this.requestReceiver = requestReceiver;
            
            // ZMQ.REP does not work asynchronously
            this.workerSocket = context.socket(ZMQ.DEALER);

            LOGGER.debug("Binding worker socket to address [{}]", inProcSocketId);
            this.workerSocket.connect(inProcSocketId);
        }
        catch(final ZMQException e)
        {
            LOGGER.error("Internal ZMQ error creating response worker socket for publisher " + publisherName, e);
            throw new LLZException("Internal ZMQ error creating response worker socket", e);
        }
    }

    /**
     * The close prepares the socket to be closed when the polling thread finish
     */
    public void prepareToStop()
    {
        this.workerSocket.setLinger(0);
    }

    @Override
    public void run()
    {
        LOGGER.debug("Beginning ZMQ RESP WORKER socket subscriber Thread for LLZ Publisher [{}]", this.publisherName);

        try
        {
            while (!Thread.currentThread().isInterrupted())
            {
                this.readRequest();
            }
        }
        catch (final ZMQException e)
        {
            if (e.getErrorCode() == LLZSocketConstants.ZSOCKET_TERM)
            {
                LOGGER.debug("Receive TERM signal by the ZMQ native on ZMQ RESP WORKER socket for LLZ Publisher [{}].", this.publisherName);
            }
            else
            {
                LOGGER.error("Unexpected internal ZMQ exception processing ZMQ RESP WORKER socket request for LLZPublisher " + this.publisherName, e);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Unexpected exception processing ZMQ RESP WORKER socket request for LLZPublisher " + this.publisherName, e);
        }


        this.closeInternalSocket();

        LOGGER.debug("ZMQ RESP WORKER socket subscriber Thread for LLZ Publisher [{}] finished", this.publisherName);
    }

    /**
     * Read and process the next incoming request
     */
    private void readRequest()
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Request received on ZMQ RESP WORKER socket for LLZ Publisher [{}] ", this.publisherName);
        }

        //  The DEALER protocol gives us the address envelope and message
        final ZMsg receivedMessage = ZMsg.recvMsg(this.workerSocket);
        final ZFrame receiveMessageAddress = receivedMessage.pop();
        final ZFrame receivedMessageContent = receivedMessage.pop();
        receivedMessage.destroy();

        // Get the data
        final byte[] msgData = receivedMessageContent.getData();
        receivedMessageContent.destroy();

        // Inform about the read request
        this.requestReceiver.onSocketReqReceived(ByteBuffer.wrap(msgData), receiveMessageAddress);
    }

    /**
     * Close the socket, this method is called when the thread that process requests is stopped when the ZMQ context is closed
     */
    private void closeInternalSocket()
    {
        LOGGER.debug("Closing ZMQ RESP WORKER socket for LLZ Publisher [{}]", this.publisherName);

        synchronized (this.lock)
        {
            try
            {
                this.workerSocket.close();
            }
            catch (final ZMQException e)
            {
                LOGGER.error("Internal ZMQ exception closing ZMQ RESP Worker socket for LLZ Publisher " + this.publisherName, e);
            }

            this.stopped = true;
        }

        LOGGER.debug("ZMQ RESP WORKER socket for LLZ Publisher [{}] closed", this.publisherName);
    }

    /**
     * Send a response directly into the ZMQ socket
     * @param responseMsg the response to send
     * @param responseAddress the ZMQ address the response should be sent to
     * @throws LLZException exception thrown if there is any problem sending the response
     */
    public void sendSocketResponse(final ByteBuffer responseMsg, final ZFrame responseAddress) throws LLZException
    {
        synchronized (this.lock)
        {
            // Make sure the socket has not been stopped already!
            if (this.stopped)
            {
                LOGGER.error("Trying to send a response on a closed socket. LLZ Publisher [{}]", this.publisherName);
                throw new LLZException("Trying to send a response on an stopped socket");
            }

            try
            {
                // Calculate the message size contained in the buffer
                final int msgSize = responseMsg.limit() - responseMsg.position();

                // Create a byte array to contain it
                final byte[] byteArrayMessage = new byte[msgSize];

                // Copy the message into the array
                responseMsg.get(byteArrayMessage, responseMsg.position(), msgSize);

                // Send the response
                responseAddress.send(this.workerSocket, ZFrame.REUSE + ZFrame.MORE);
                final ZFrame response = new ZFrame(byteArrayMessage);
                response.send(this.workerSocket, ZFrame.REUSE);
            }
            catch (final ZMQException e)
            {
                LOGGER.error("Internal ZMQ exception trying to send a response", e);
                throw new LLZException("Internal ZMQ exception sending response", e);
            }
        }
    }
}
