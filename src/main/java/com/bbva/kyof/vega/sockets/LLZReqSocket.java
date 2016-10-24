package com.bbva.kyof.vega.sockets;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.serialization.UUIDSerializer;

/**
 * Wrapper to handle a request socket in ZMQ. Requester sockets can send request and listen to responses
 *
 * This is thread-safe
 */
public final class LLZReqSocket implements Runnable
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZReqSocket.class);

    /** Name of the LLZ Publisher the socket belongs to */
    private final String requesterName;

    /** Listener to handle responses coming from this socket */
    private final ILLZReqSocketRespHandler responseListener;

    /** ZMQ real socket */
    private final ZMQ.Socket clientSocket;

    /** Socket transport */
    private final String socketTransport;

    /** True if the requester socket should be stopped */
    private volatile boolean shouldStop = false;

    /** True if the requester is stopped */
    private volatile boolean stopped = false;

    /** Lock for class access */
    private final Object lock = new Object();

    /**
     * Create a new socket
     *
     * @param context the ZMQ context for the socket
     * @param reqTransport the transport for the socket
     * @param requesterName the name of the publisher the socket belongs to
     * @param responseListener the listener for responses
     * @throws LLZException exception thrown if there is any issue during the socket creation
     */
    private LLZReqSocket(final ZMQ.Context context,
                        final String reqTransport,
                        final String requesterName,
                        final ILLZReqSocketRespHandler responseListener) throws LLZException
    {
        this.requesterName = requesterName;
        this.responseListener = responseListener;
        this.socketTransport = reqTransport;

        try
        {
            // Create the dealer socket that will handle the requests
            this.clientSocket = context.socket(ZMQ.DEALER);

            // Create a random identity
            this.clientSocket.setIdentity(this.createRandomIdentity());

            // Perform the connection
            this.clientSocket.connect(reqTransport);
        }
        catch(final ZMQException e)
        {
            LOGGER.error("Internal ZMQ error creating REQ socket", e);
            throw new LLZException("Unexpected internal ZMQ exception creating REQ socket", e);
        }
    }

    /**
     * Create a new socket
     *
     * @param context the ZMQ context for the socket
     * @param reqTransport the transport for the socket
     * @param publisherName the name of the publisher the socket belongs to
     * @param responseListener the listener for responses
     * @return the created sockect
     * @throws LLZException exception thrown if there is any issue during the socket creation
     */
    public static LLZReqSocket createNewSocket(final ZMQ.Context context,
                                               final String reqTransport,
                                               final String publisherName,
                                               final ILLZReqSocketRespHandler responseListener) throws LLZException
    {
        LOGGER.debug("Creating ZMQ REQ socket for LLZ Publisher [{}] and interface [{}]", publisherName, reqTransport);

        // Create the result socket
        final LLZReqSocket result = new LLZReqSocket(context, reqTransport, publisherName, responseListener);

        // Start the pooling for responses
        final Thread responseReceiverThread = new Thread(result, "LLZ REQ SOCKET " + publisherName);
        responseReceiverThread.start();

        return result;
    }

    @Override
    public void run()
    {
        LOGGER.debug("Beginning ZMQ REQ socket publisher Thread for LLZ Publisher [{}]", this.requesterName);

        try
        {
            // Create and register a poller that will try to get a single element
            ZMQ.Poller poller = new ZMQ.Poller(1);
            poller.register(this.clientSocket);

            while(!this.shouldStop)
            {
                this.processNextResponse(poller);
            }
        }
        catch (final ZMQException e)
        {

            LOGGER.error("Unexpected internal ZMQ exception processing ZMQ REQ socket responses for LLZPublisher " + this.requesterName, e);
        }
        catch (final Exception e)
        {
            LOGGER.error("Unexpected exception processing ZMQ REQ socket responses for LLZPublisherr " + this.requesterName, e);
        }

        // Close the socket
        this.internalCloseSocket();

        LOGGER.debug("ZMQ REQ socket publisher Thread for LLZ Publisher [{}] finished", this.requesterName);
    }

    private void processNextResponse(final ZMQ.Poller poller)
    {
        ByteBuffer receivedBuffer;

        // Synchronize the access to the socket
        synchronized (this.lock)
        {
            // TODO, see if we can reduce the polling time to 0 to prevent the sending of requests from being blocked without killing performance
            // Poll with a timeout of one millisecond
            if (poller.poll(1) == -1)
            {
                LOGGER.error("Polling thread for requester [{}] interrupted", this.requesterName);

                // Set the socket to stop
                this.shouldStop = true;
                return;
            }

            // Get and process the first element if there is something
            if (poller.pollin(0))
            {
                final byte[] rcvMessage = this.clientSocket.recv(ZMQ.DONTWAIT);
                receivedBuffer = ByteBuffer.wrap(rcvMessage);
            }
            else
            {
                receivedBuffer = null;
            }
        }

        // Send the response to the listener
        if (receivedBuffer != null)
        {
            this.responseListener.onSocketRespReceived(receivedBuffer);
        }
    }

    /**
     * Send the given request contained in the buffer
     *
     * @param buffer buffer containing the request to send
     */
    public void sendRequest(final ByteBuffer buffer) throws LLZException
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("ZMQ REQ socket for LLZ Publisher [{}] sending message on transport [{}] ", this.requesterName, this.socketTransport);
        }

        synchronized (this.lock)
        {
            try
            {
                if (this.shouldStop || this.stopped)
                {
                    LOGGER.error("Trying to send a request on a closed socket. LLZ Publisher [{}]", this.requesterName);
                    throw new LLZException("Trying to send a message on a closed socket");
                }

                this.clientSocket.send(buffer.array(), buffer.position(), buffer.limit(), 0);
            }
            catch (final ZMQException e)
            {
                LOGGER.error("Unexpected internal ZMQ error sending request", e);
                throw new LLZException("Internal ZMQ error sending request", e);
            }
        }
    }

    /**
     * Stop the requester socket
     */
    public void stop() throws InterruptedException
    {
        LOGGER.debug("Closing ZMQ REQ socket for LLZ Requester [{}] for closing", this.requesterName);

        this.shouldStop = true;

        while(!this.stopped)
        {
            Thread.sleep(1);
        }
    }

    /**
     * Close the internal socket, this method is called on the internal thread that process messages when the ZMQ Context is closed
     */
    private void internalCloseSocket()
    {
        LOGGER.debug("Closing ZMQ REQ socket for LLZ Publisher [{}] and interface [{}]", this.requesterName, this.socketTransport);

        synchronized (this.lock)
        {
            try
            {
                this.clientSocket.setLinger(0);
                this.clientSocket.close();
            }
            catch (final ZMQException e)
            {
                LOGGER.error("Internal ZMQ exception closing ZMQ REQ socket for LLZ Publisher " + this.requesterName, e);
            }

            this.stopped = true;
        }

        LOGGER.debug("ZMQ REQ socket for LLZ Publisher [{}] and interface [{}] closed", this.requesterName, this.socketTransport);
    }

    /**
     * @return a new random identity for requests sockets
     */
    private byte[] createRandomIdentity()
    {
        final ByteBuffer result = ByteBuffer.allocate(UUIDSerializer.uniqueIdSerializedSize()+1);
        // Identities starting with binary zero are reserved for use by ZMQ infrastructure.
        result.put((byte)1);
        UUIDSerializer.uniqueIdToBinary(UUID.randomUUID(), result);
        return result.array();
    }
}
