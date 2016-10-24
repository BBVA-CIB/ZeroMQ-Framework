package com.bbva.kyof.vega.config;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.sockets.LLZSocketConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.nio.ByteBuffer;

/**
 * This class wrapper the behaviour of a ZMQ publisher socket.
 *
 * The pub socket is thread-safe.
 */
public final class LLZTopicSchema
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZTopicSchema.class);

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
    public LLZTopicSchema(final ZMQ.Context context,
                          final String publisherName,
                          final String strInterface,
                          final Long rateLimit) throws LLZException
    {
        LOGGER.debug("Creating ZMQ PUB socket for LLZ Publisher [{}] and interface [{}]", publisherName, strInterface);

        this.publisherName = publisherName;
        this.socketInterface = strInterface;

        try
        {
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

            // Bind the socket to the given network interface
            this.nativeZMQSocket.bind(strInterface);
        }
        catch(final ZMQException e)
        {
            LOGGER.error("Internal ZMQ error binging publisher socket to interface:" + strInterface, e);
            throw new LLZException("ZMQ internal exception binding publisher socket to interface", e);
        }
    }

    /**
     * Stop and close the socket
     *
     * @throws LLZException exception thrown if there are problems closing the socket
     */
    public void stopAndClose() throws LLZException
    {
        LOGGER.debug("Closing ZMQ PUB socket for LLZ Publisher [{}] and interface [{}]", this.publisherName, this.socketInterface);

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

        LOGGER.debug("Closed ZMQ PUB socket for LLZ Publisher [{}] and interface [{}]", this.publisherName, this.socketInterface);
    }

    /**
     * Send a message into the socket.
     *
     * This process consist in sending two messages, one for the topic and other for the message.
     *
     * @param topic topic to send the message to
     * @param message  Message that belongs to the topic passed in the first parameter.
     * @throws LLZException if there is any problem with the sending
     */
    public void send(final String topic, final ByteBuffer message) throws LLZException
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("ZMQ PUB socket for LLZ Publisher [{}] sending message on interface [{}] ", this.publisherName, this.socketInterface);
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
                this.nativeZMQSocket.sendMore(topic);
                this.nativeZMQSocket.send(message.array(), message.position(), message.limit(), 0);
            }
            catch (final ZMQException e)
            {
                LOGGER.error("ZMQ internal exception trying to publish a message. LLZ Publisher :[" + this.publisherName + "]", e);
                throw new LLZException("Internal ZMQ exception publishing on LLZ Publisher :[" + this.publisherName + "]", e);
            }
        }
    }
}
