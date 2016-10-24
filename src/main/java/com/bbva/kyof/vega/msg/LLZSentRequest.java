package com.bbva.kyof.vega.msg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents the information of a sent request.
 *
 * The request should be always manually closed if the expiration time is set to 0.
 *
 * In any case is a good practice to close requests when no more responses are expected.
 *
 * This class is thread safe!
 */
public class LLZSentRequest implements ILLZSentRequest
{
    /** LOGGER Instance */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZSentRequest.class);

    /** Unique identifier of the request */
    private final UUID requestId;
    
    /** Request expiration time */
    private final long expirationTime;
    
    /** Listener for timeouts on this request */
    private final ILLZReqTimeoutListener timeoutListener;
    
    /** Listener for responses on this request */
    private final ILLZTopicRespListener responseListener;
    
    /** Number of received responses */
    private final AtomicInteger numResponses = new AtomicInteger();
    
    /** The topic the request belongs to */
    private final String topic;
    
    /** True if the request has been closed */
    private boolean closed = false;

    /**
     * Constructor of the sent request information
     *
     * @param topic the topic the request belong to
     * @param timeout timeout for the request expiration, 0 for no expiration time
     * @param timeoutListener listener for timeouts
     * @param responseListener listener for responses
     * @param rndGenerator random number generator that will be used to create the unique ID of the request
     */
    public LLZSentRequest(final String topic,
                          final long timeout,
                          final ILLZReqTimeoutListener timeoutListener,
                          final ILLZTopicRespListener responseListener,
                          final Random rndGenerator)
    {
        this.topic = topic;
        this.requestId = new UUID(rndGenerator.nextLong(), rndGenerator.nextLong());
        this.timeoutListener = timeoutListener;
        this.responseListener = responseListener;

        if (timeout == 0)
        {
            this.expirationTime = 0;
        }
        else
        {
            this.expirationTime = System.currentTimeMillis() + timeout;
        }
    }

    @Override
    public UUID getRequestId()
    {
        return requestId;
    }

    @Override
    public boolean hasExpired()
    {
        return (this.expirationTime != 0) && (System.currentTimeMillis() > expirationTime);
    }

    @Override
    public void closeRequest()
    {
        synchronized (this)
        {
            this.closed = true;
        }
    }

    @Override
    public boolean isClosed()
    {
        synchronized (this)
        {
            return this.closed;
        }
    }

    @Override
    public int getNumberOfResponses()
    {
        return this.numResponses.get();
    }

    /**
     * Process a received response and send it to the response listener if the request has not been closed yet
     *
     * @param response the received response
     */
    public void onResponseReceived(final LLZRcvResponse response)
    {
        synchronized (this)
        {
            // If already closed ignore the response
            if (this.closed)
            {
                LOGGER.info("Response received on an already closed or expired request. Request ID [{}], Responder AppId [{}]", this.requestId, response.getInstanceId());
                return;
            }

            // Increment the number of received responses
            this.numResponses.getAndIncrement();

            // If not look for a response listener and send the response
            if (this.responseListener != null)
            {
                try
                {
                    this.responseListener.onResponseReceived(this, response);
                }
                catch (final Exception e)
                {
                    LOGGER.error("Uncaught exception processing received response for request ID " + this.requestId, e);
                }
            }
        }
    }

    /**
     * Called when the request times out, a timeout request should be closed if not already closed and the timeout listener notified if exists.
     */
     public void onRequestTimeout()
    {
        synchronized (this)
        {
            // If already closed just return
            if (this.closed)
            {
                return;
            }

            // Set the request as closed
            this.closed = true;

            // If there is a timeout listener notify about the closure
            if (this.timeoutListener != null)
            {
                try
                {
                    this.timeoutListener.onRequestTimeout(this);
                }
                catch (final Exception e)
                {
                    LOGGER.error("Uncaught exception processing request timeout for request id " + this.requestId, e);
                }
            }
        }
    }

    /** @return the topic associated tot he request  */
    public String getTopic()
    {
        return this.topic;
    }
}
