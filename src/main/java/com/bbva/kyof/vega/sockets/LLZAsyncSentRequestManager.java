package com.bbva.kyof.vega.sockets;

import com.bbva.kyof.vega.msg.ILLZReqTimeoutListener;
import com.bbva.kyof.vega.msg.ILLZTopicRespListener;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZRcvResponse;
import com.bbva.kyof.vega.msg.LLZSentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Daemon class that will periodically check for timeouts in the requests that have been sent. </br>
 *
 * Requests that have timed out will be deleted and closed, or the listener will be called if settled.
 *
 * @author xe27609
 */
public class LLZAsyncSentRequestManager implements Runnable
{
    /** Sleeping time between consecutive timeout checks */
    private static final long SLEEP_TIME = 1;

    /** Logger for the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZAsyncSentRequestManager.class);

    /** True if the daemon should stop */
    private volatile boolean shouldStop = false;

    /** True if the daemon is stopped */
    private volatile boolean isStopped = false;

    /** Map of requests by request ID */
    private final Map<UUID, LLZSentRequest> requestById = new ConcurrentHashMap<>();

    /** Random number generator */
    private final Random rnd = new Random(System.currentTimeMillis());

    /**
     * Create and start the daemon
     */
    public LLZAsyncSentRequestManager()
    {
        // Create a new thread for the timeout daemon
        final Thread timeoutThread = new Thread(this, "LLZ ASYNC SENT REQ");
        timeoutThread.start();
    }

    /**
     * Create and add a new sent request into the manager
     *
     * @param topic the topic the request belongs to
     * @param timeout the timeout for the request
     * @param responseListener the listener for responses
     * @param timeoutListener the listener for timeouts, it can be null
     * @return the created sent request that has been internally added
     */
    public LLZSentRequest addNewRequest(
            final String topic,
            final long timeout,
            final ILLZTopicRespListener responseListener,
            final ILLZReqTimeoutListener timeoutListener)
    {
        final LLZSentRequest sentRequest = new LLZSentRequest(topic, timeout, timeoutListener, responseListener, this.rnd);
        this.requestById.put(sentRequest.getRequestId(), sentRequest);
        return sentRequest;
    }

    /** Stops the checking, wait for it to finish and clean the pending requests */
    public void stopAndWaitToFinish()
    {
        LOGGER.debug("Stopping request manager thread...");

        // Change the status to force the thread to stop
        this.shouldStop = true;

        // Make sure we wait for the thread to stop
        while (!this.isStopped)
        {
            try
            {
                Thread.sleep(SLEEP_TIME);
            }
            catch (InterruptedException e)
            {
                LOGGER.error("Unexpected interruption while waiting for the daemon to finish", e);
            }
        }

        // Clear any pending request in the list
        this.closePendingRequests();
    }

    /**
     * Process a received response, it will look for the original request message and pass the response
     *
     * @param response the response to process
     */
    public void onResponseReceived(final LLZMsgHeader messageHeader, final ByteBuffer response)
    {
        // Look for the original request
        final LLZSentRequest originalRequest = this.requestById.get(messageHeader.getRequestId());

        if (originalRequest != null)
        {
            // Get the topic from the original request
            final String topic = originalRequest.getTopic();
            originalRequest.onResponseReceived(new LLZRcvResponse(messageHeader, response, topic));
        }
    }

    @Override
    public void run()
    {
        LOGGER.debug("Request manager timeout thread started");

        try
        {
            // While it should not stop
            while(this.shouldContinue())
            {
                // Check for possible timeouts
                this.checkForTimeouts();

                // Sleep a little bit to reduce CPU usage
                Thread.sleep(SLEEP_TIME);
            }
        }
        catch (InterruptedException e)
        {
            LOGGER.error("Unexpected interruption in the timeout checking process", e);
        }
        finally
        {
            this.isStopped = true;
        }

        LOGGER.debug("Request manager timeout thread stopped");
    }

    /** @return true if the daemon should continue checking */
    private boolean shouldContinue()
    {
        return !this.shouldStop;
    }

    /** Check for possible timeouts in the stored requests */
    private void checkForTimeouts()
    {
        // If there are no request just return
        if (this.requestById.isEmpty())
        {
            return;
        }

        // Get an iterator for the list of requests and go through them
        final Iterator<Map.Entry<UUID, LLZSentRequest>> iterator = this.requestById.entrySet().iterator();
        while (iterator.hasNext())
        {
            final LLZSentRequest request = iterator.next().getValue();

            // If the request has timed out remove it and notify if there is a listener
            if (request.hasExpired())
            {
                iterator.remove();
                request.onRequestTimeout();
            }
            else if (request.isClosed())
            {
                // If it has been manually closed but not expired yet remove it as well
                iterator.remove();
            }
        }
    }

    /** Close all pending requests in the stored requests */
    private void closePendingRequests()
    {
        // Iterate over the requests and close them
        for (final LLZSentRequest request : this.requestById.values())
        {
            request.closeRequest();
        }

        // Clean the memory
        this.requestById.clear();
    }
}
