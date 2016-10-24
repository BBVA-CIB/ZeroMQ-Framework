package com.bbva.kyof.vega.sockets;

import java.nio.ByteBuffer;

import com.bbva.kyof.vega.exception.LLZExceptionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.bbva.kyof.vega.exception.LLZException;

/**
 * This class wrapper the behaviour of a ZMQ publisher socket.
 *
 * The pub socket is thread-safe.
 */
public final class LLZPubSocket
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZPubSocket.class);

    /** Name of the publisher */
    private final String publisherName;

    /** The instance of the ZMQ Socket*/
    private final ZMQ.Socket nativeZMQSocket;

    /** Socket interface */
    private final String socketInterface;

    /** True if the socket has been stopped */
    private boolean stopped = false;

    /** Lock for class access */
    private final Object lock = new Object();
    
    /** Current port in use */
    private int currentPort;
    
    /** Range of available ports */
    private final int numPortsInRange;

    /** Minimum port of range of available ports */
    private final int minPort;

    /** Maximum port of range of available ports */
    private final int maxPort;


    /**
     * Construct and initialize the socket
     *
     * @param context the main context of the instance
     * @param publisherName the name of the publisher
     * @param strInterface the interface of the socket in ZMQ interface string format
     * @param rateLimit receive rate limit for the socket, null to use default values
     *
     * @throws LLZException exception thrown if there is any problem
     */
    public LLZPubSocket(final ZMQ.Context context,
                        final String publisherName,
                        final String strInterface,
                        final Long rateLimit,
                        final int minPort, 
                        final int maxPort) throws LLZException
    {
        LOGGER.debug("Creating ZMQ PUB socket for LLZ Publisher [{}] and interface [{}]", publisherName, strInterface);

        this.publisherName = publisherName;
        this.socketInterface = strInterface;
        this.currentPort = minPort;
        this.numPortsInRange = (maxPort - minPort) + 1;
        this.minPort = minPort;
        this.maxPort = maxPort;
               
        // Create the native socket
        this.nativeZMQSocket = context.socket(ZMQ.PUB);

        // Set the reception rate limit, it will drop messages when the limit is reached
        if (rateLimit == null)
        {
            this.nativeZMQSocket.setSndHWM(LLZSocketConstants.DEFAULT_RATE_LIMIT);
        }
        else
        {
            this.nativeZMQSocket.setSndHWM(rateLimit);
        }   

        this.tryBinding();  
    }

    /**
     *  Tries to bind a socket to some port available
     * @throws LLZException if it cannot bind socket to port cause address is already in use
     */
    private void tryBinding() throws LLZException
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
                this.nativeZMQSocket.bind(this.socketInterface + this.currentPort);
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
        LOGGER.error("Internal ZMQ error binding publisher socket to interface. {}:{}. Trying to close socket...", this.socketInterface, this.currentPort);

        // Try to close the socket
        try
        {
            this.nativeZMQSocket.close();

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
            throw new LLZException(errorMsg, LLZExceptionCode.NO_AVAILABLE_PORTS);
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

    /**
     * Stop and close the socket
     *
     * @throws LLZException exception thrown if there are problems closing the socket
     */
    public void stopAndClose() throws LLZException
    {
        LOGGER.debug("Closing ZMQ PUB socket for LLZ Publisher [{}] and interface [{}]", this.publisherName, this.socketInterface + this.currentPort);

        synchronized (this.lock)
        {
            if (this.stopped)
            {
                LOGGER.error("Trying to close a pub socket that is already closed");
                throw new LLZException("The socket is already closed");
            }

            try
            {
                this.nativeZMQSocket.close();
                this.stopped = true;
            }
            catch (final ZMQException e)
            {
                LOGGER.error("Error closing publisher socket: " + this.publisherName, e);
                throw new LLZException("Error closing publisher sockets", e);
            }
        }

        LOGGER.debug("Closed ZMQ PUB socket for LLZ Publisher [{}] and interface [{}]", this.publisherName, this.socketInterface + this.currentPort);
    }

    /**
     * Send a message into the socket.
     *
     * This process consist in sending two messages, one for the topic and other for the message.
     *
     * @param message  Message that belongs to the topic passed in the first parameter.
     * @throws LLZException if there is any problem with the sending
     */
    public void send(final ByteBuffer message) throws LLZException
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("ZMQ PUB socket for LLZ Publisher [{}] sending message on interface [{}] ", this.publisherName, this.socketInterface + this.currentPort);
        }

        synchronized (this.lock)
        {
            if (this.stopped)
            {
                LOGGER.error("Trying to send a message on a closed socket. LLZ Publisher [{}]", this.publisherName);
                throw new LLZException("Trying to send a message on a closed socket");
            }

            try
            {
                this.nativeZMQSocket.send(message.array(), message.position(), message.limit(), 0);
            }
            catch (final ZMQException e)
            {
                LOGGER.error("ZMQ internal exception trying to publish a message. LLZ Publisher :[" + this.publisherName + "]", e);
                throw new LLZException("Internal ZMQ exception publishing on LLZ Publisher :[" + this.publisherName + "]", e);
            }
        }
    }

    /** @return current port in use */
    public int getCurrentPort()
    {
        return this.currentPort;
    }


}
