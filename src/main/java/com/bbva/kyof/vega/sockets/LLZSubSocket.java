package com.bbva.kyof.vega.sockets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.bbva.kyof.vega.exception.LLZException;

/**
 * Wrapper for a subscriber ZMQ socket
 *
 * This class is thread-safe
 */
public final class LLZSubSocket implements Runnable
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZSubSocket.class);

    /** UTF-8 char set */
    public static final Charset UTF8_CHAR_SET = Charset.forName("UTF-8");

    /** The instance of the ZMQ native Socket */
    private final ZMQ.Socket nativeZMQSocket;

    /** Handler to process incoming messages */
    private final ILLZSubSocketRcvHandler receiveHandler;

    /** Lock for class access */
    private final Object lock = new Object();

    /** Physical transport of the subscriber socket */
    private final String subTransport;

    /** True if the socket should be stopped */
    private volatile boolean shouldStop = false;

    /** True if the socket has been stopped */
    private volatile boolean stopped = false;

    /**
     * Create the subscriber socket and start the pooling
     *
     * @param context original ZMQ context
     * @param subTransport transport connection string for the socket
     * @param receiveHandler handler to send the received messages
     * @param rateLimit receive rate limit for the socket, null to use default values
     */
    public LLZSubSocket(final ZMQ.Context context,
                        final String subTransport,
                        final ILLZSubSocketRcvHandler receiveHandler,
                        final Long rateLimit) throws LLZException
    {
        LOGGER.debug("Creating ZMQ SUB socket for LLZ Subscriber and transport [{}]",  subTransport);

        this.subTransport = subTransport;
        this.receiveHandler = receiveHandler;

        try
        {
            // Initialization of the ZMQ Socket
            this.nativeZMQSocket = context.socket(ZMQ.SUB);

            // Set the reception rate limit, it will drop messages when the limit is reached
            if (rateLimit == null)
            {
                this.nativeZMQSocket.setRcvHWM(LLZSocketConstants.DEFAULT_RATE_LIMIT);
            }
            else
            {
                this.nativeZMQSocket.setRcvHWM(rateLimit);
            }

            // Perform the connections
            this.nativeZMQSocket.connect(subTransport);

            // Subscribe to all (ZMQ API docs)
            this.nativeZMQSocket.subscribe("".getBytes(UTF8_CHAR_SET));
        }
        catch (final ZMQException e)
        {
            LOGGER.error("Unexpected internal ZMQ exception creating ZMQ sub socket", e);
            throw new LLZException("Internal ZMQ exception creating ZMQ sub socket", e);
        }

        // Start the pooling thread if there have been no errors
        final Thread pollingThread = new Thread(this, "LLZ SUB SOCKET");
        pollingThread.start();
    }

    private void processNextMsg(final ZMQ.Poller poller)
    {
        ByteBuffer receivedBuffer;

        // Synchronize the access to the socket
        synchronized (this.lock)
        {
            // Poll with a timeout of one millisecond
            if (poller.poll(1) == -1)
            {
                LOGGER.error("Polling thread for subscriber [{}] interrupted", this.subTransport);

                // Set the socket to stop
                this.shouldStop = true;
                return;
            }

            // Get and process the first element if there is something
            if (poller.pollin(0))
            {
                final byte[] rcvMessage = this.nativeZMQSocket.recv(ZMQ.DONTWAIT);
                receivedBuffer = ByteBuffer.wrap(rcvMessage);
            }
            else
            {
                receivedBuffer = null;
            }
        }

        // Send the message to the listener
        if (receivedBuffer != null)
        {
            // Send the message to the handler
            this.receiveHandler.onSocketMsgReceived(receivedBuffer);
        }
    }

    /**
     * Stop the requester socket
     */
    public void stop() throws InterruptedException
    {
        LOGGER.debug("Closing ZMQ SUB socket on transport [{}]", this.subTransport);

        this.shouldStop = true;

        while(!this.stopped)
        {
            Thread.sleep(1);
        }
    }

    @Override
    public void run()
    {
        LOGGER.debug("Beginning ZMQ PUB socket subscriber Thread for LLZ Subscriber");

        try
        {
            // Create and register a poller that will try to get a single element
            ZMQ.Poller poller = new ZMQ.Poller(1);
            poller.register(this.nativeZMQSocket);

            while(!this.shouldStop)
            {
                this.processNextMsg(poller);
            }
        }
        catch (final ZMQException e)
        {
            LOGGER.error("Unexpected internal ZMQ exception processing ZMQ SUB socket message for LLZSubscriber", e);
        }
        catch (final Exception e)
        {
            LOGGER.error("Unexpected exception processing ZMQ SUB socket message for LLZSubscriber", e);
        }

        // Try to close the socket before exiting
        this.internalCloseSocket();

        LOGGER.debug("ZMQ PUB socket subscriber Thread for LLZ Subscriber finished");
    }

    /**
     * Close protocol when context is closed and TERM signal sent
     */
    private void internalCloseSocket()
    {
        LOGGER.debug("Closing ZMQ SUB socket for LLZ Subscriber and transport [{}]", this.subTransport);

        synchronized (this.lock)
        {
            try
            {
                this.nativeZMQSocket.setLinger(0);
                this.nativeZMQSocket.close();
            }
            catch (final ZMQException e)
            {
                LOGGER.error("Error closing subscriber socket", e);
            }

            this.stopped = true;
        }

        LOGGER.debug("ZMQ SUB socket for LLZ Subscriber and transport [{}] closed", this.subTransport);
    }
}
