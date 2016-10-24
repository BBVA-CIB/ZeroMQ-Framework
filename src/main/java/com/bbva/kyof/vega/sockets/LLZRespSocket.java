package com.bbva.kyof.vega.sockets;

import com.bbva.kyof.vega.exception.LLZException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Wrapper to handle a responder socket in ZMQ. Responder sockets listen for request and are able to send responses.
 *
 * It will create a ROUTER Front and DEALER backend, proxy both of them and then connect the dealer with
 * a worker socket that will handle the responses asynchronously. This is a classic ZMQ pattern.
 */
public final class LLZRespSocket implements Runnable
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZRespSocket.class);

    /** Name of the responder the socket belongs to */
    private final String responderName;

    /** Address the front end router ZMQ socket will be connected to */
    private final String socketInterface;

    /** The instance of the ZMQ Socket that acts as a Router and frontend */
    private ZMQ.Socket frontendSocket = null;

    /** The instance of the ZMQ Socket that acts as Dealer in the backend, giving the request to a worker node*/
    private ZMQ.Socket backendSocket = null;

    /** Worker sockets to handle received requests */
    private LLZRespWorkerSocket workerSocket;

    /** Current port in use */
    private int currentPort;

    /** Range of available ports */
    private final int numPortsInRange;

    /** Minimum port of range of available ports */
    private final int minPort;

    /** Maximum port of range of available ports */
    private final int maxPort;

    /**
     * Constructs a new resp socket wrapper, bind the front and backend and start the worker socket threading
     *
     * @param context ZMQ context
     * @param responderName The name of the publisher this socket belongs to
     * @param socketInterface address the router should be connected to to listen for responses, without the port
     * @param receivedRequestHandler handler that will receive incoming requests from this socket
     *
     * @throws LLZException exception thrown if there is a problem creating the socket
     */
    private LLZRespSocket(final ZMQ.Context context,
                         final String responderName,
                         final String socketInterface,
                         final ILLZRespSocketReqHandler receivedRequestHandler,
                         final int minPort,
                         final int maxPort) throws LLZException
    {
        this.responderName = responderName;
        this.socketInterface = socketInterface;
        this.currentPort = minPort;
        this.numPortsInRange = (maxPort - minPort) + 1;
        this.minPort = minPort;
        this.maxPort = maxPort;

        final String inProcSocketId = this.getRndSocketInProcId();

        // Create the router socket and bind it
        this.frontendSocket = context.socket(ZMQ.ROUTER);
        this.tryBindingFrontEndSocket();

        try
        {
            // Create dealer, bind to router and finally create the worker socket that will process the requests
            this.backendSocket = context.socket(ZMQ.DEALER);

            // Bind the backend to route the requests to workers
            this.backendSocket.bind(inProcSocketId);

            // Create the worker sockets and launch the thread
            this.workerSocket = new LLZRespWorkerSocket(context, responderName, inProcSocketId, receivedRequestHandler);
        }
        catch(final ZMQException e)
        {
            LOGGER.error("Internal ZMQ exception creating the RESP socket", e);
            throw new LLZException("Unexpected internal ZMQ exception creating RESP socket", e);
        }
    }

    /**
     * Constructs a new resp socket wrapper, bind the front and backend and start the worker socket threading
     *
     * @param context ZMQ context
     * @param publisherName The name of the publisher this socket belongs to
     * @param strAddress address the router should be connected to to listen for responses
     * @param receivedRequestHandler handler that will receive incoming requests from this socket
     *
     * @throws LLZException exception thrown if there is a problem creating the socket
     */
    public static LLZRespSocket createNewSocket(final ZMQ.Context context,
                                                final String publisherName,
                                                final String strAddress,
                                                final ILLZRespSocketReqHandler receivedRequestHandler,
                                                final int minReqPort,
                                                final int maxReqPort) throws LLZException
    {
        LOGGER.debug("Creating ZMQ RESP socket for LLZ Publisher [{}] and interface [{}]", publisherName, strAddress);

        // Create the result socket
        final LLZRespSocket result = new LLZRespSocket(context, publisherName, strAddress, receivedRequestHandler, minReqPort, maxReqPort);

        // Launch the worker socket thread
        final Thread workerSocketThread = new Thread(result.workerSocket, "LLZ RESP WORKER SOCKET " + publisherName);
        workerSocketThread.start();

        // Launch the proxy thread to bind the sockets
        final Thread proxyThread = new Thread(result, "LLZ RESP PROXY SOCKET " + publisherName);
        proxyThread.start();

        return result;
    }

    /**
     * The stop prepares the socket to be closed when the polling thread finish by changing the linger value
     */
    public void prepareToStop()
    {
        LOGGER.debug("Prepare ZMQ RESP socket for LLZ Publisher [{}] for closing", this.responderName);

        this.frontendSocket.setLinger(0);
        this.backendSocket.setLinger(0);
        this.workerSocket.prepareToStop();
    }

    /**
     * Send a response directly into the ZMQ worker socket
     *
     * @param responseMsg the response to send
     * @param responseAddress the ZMQ address the response should be sent to
     * @throws LLZException exception thrown if there is any problem sending the response
     */
    public void sendSocketResponse(final ByteBuffer responseMsg, final ZFrame responseAddress) throws LLZException
    {
        this.workerSocket.sendSocketResponse(responseMsg, responseAddress);
    }

    @Override
    public void run()
    {
        LOGGER.debug("Beginning ZMQ RESP socket proxy Thread for LLZ Publisher [{}]", this.responderName);

        try
        {
            // Finally proxy the frontend and backends, it will block until the context is closed
            ZMQ.proxy(this.frontendSocket, this.backendSocket, null);
        }
        catch(final ZMQException e)
        {
            LOGGER.error("Internal ZMQ exception binding the proxy thread of the RESP Socket for LLZ Publisher " + this.responderName, e);
        }

        // The proxy thread has been freed, close the socket
        this.internalCloseSockets();

        LOGGER.debug("ZMQ RESP socket proxy Thread for LLZ Publisher [{}] finished", this.responderName);
    }

    /**
     * Close the internal socket, this method is called on the internal thread that proxies router and dealer when the ZMQ Context is closed
     */
    private void internalCloseSockets()
    {
        LOGGER.debug("Closing ZMQ RESP socket for LLZ Publisher [{}] and interface [{}]", this.responderName, this.socketInterface);

        try
        {
            if (this.frontendSocket != null)
            {
                this.frontendSocket.close();
                this.frontendSocket = null;
            }

            if (this.backendSocket != null)
            {
                this.backendSocket.close();
                this.backendSocket = null;
            }

            this.workerSocket = null;
        }
        catch (final ZMQException e)
        {
            LOGGER.error("Unexpected ZMQ Exception closing ZMQ RESP socket for LLZ Publisher " + this.responderName, e);
        }

        LOGGER.debug("ZMQ RESP socket for LLZ Publisher [{}] and interface [{}] closed", this.responderName, this.socketInterface);
    }

    /**
     * Return a new random identifier for protocol inter process communication
     */
    private String getRndSocketInProcId()
    {
        // Sync the call just in case the implementation is not thread safe
        return "inproc://" + UUID.randomUUID().toString();
    }

    /**
     * Returns port which this socket was bound to
     * @return port
     */
    public int getCurrentPort()
    {
        return this.currentPort;
    }

    /**
     *  Tries to bind a socket to some port available
     * @throws LLZException if it cannot bind socket to port cause address is already in use
     */
    private void tryBindingFrontEndSocket() throws LLZException
    {
        int numPortsTried = 0;

        // Flag to check if socket has been bound properly
        boolean bound = false;

        // The first step is to get the real bind Ip
        while (numPortsTried <  this.numPortsInRange  && !bound)
        {
            try
            {
                // ZMQ throws exception if error, no need to implement max tries (maybe retry)
                // Bind the socket to the given network interface
                this.frontendSocket.bind(this.socketInterface + this.currentPort);
                bound = true;
            }
            catch(final ZMQException exception)
            {
                // If the address is already in use
                if (exception.getErrorCode() == ZMQ.Error.EADDRINUSE.getCode())
                {
                    numPortsTried++;
                    this.advanceCurrentPort(numPortsTried);
                }
                else
                {
                    this.closeSocketAfterCreationError(exception);
                }
            }
        }
    }

    private void closeSocketAfterCreationError(final ZMQException originalException) throws LLZException
    {
        LOGGER.error("Internal ZMQ error binding responder socket to interface. {}:{}. Trying to close socket...", this.socketInterface, this.currentPort);

        // Try to close the socket
        try
        {
            this.frontendSocket.close();

            // Launch the original exception after closing the socket
            throw new LLZException(originalException);
        }
        catch (final ZMQException e)
        {
            LOGGER.error("Internal ZMQ error trying to close socket after binding failure", e);
            throw new LLZException("Internal ZMQ error trying to close socket after binding failure", e);
        }
    }

    /**
     *  Advances the current port in use
     * @param numPortsTried
     */
    private void advanceCurrentPort(int numPortsTried) throws LLZException
    {
        LOGGER.debug("Cannot bind socket to port, address already in use. Address [{}]. Trying with next port.", this.socketInterface + this.currentPort);

        // Make sure there are tries left
        if (numPortsTried >= this.numPortsInRange)
        {
            final String errorMsg = String.format("Cannot find any free port in range [%d-%d]", this.minPort, this.maxPort);
            LOGGER.error(errorMsg);
            throw new LLZException(errorMsg);
        }

        // Advance the current port position
        if (this.currentPort == this.maxPort)
        {
            this.currentPort = this.minPort;
        }
        else
        {
            this.currentPort++;
        }
    }
}
