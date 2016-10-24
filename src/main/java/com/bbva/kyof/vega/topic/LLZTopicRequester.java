package com.bbva.kyof.vega.topic;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.ILLZReqTimeoutListener;
import com.bbva.kyof.vega.msg.ILLZSentRequest;
import com.bbva.kyof.vega.msg.ILLZTopicRespListener;
import com.bbva.kyof.vega.msg.LLZSentRequest;
import com.bbva.kyof.vega.sockets.LLZAsyncSentRequestManager;

/**
 * The topicName publisher is the class that conglomerates all the functionality to publish messages into a topicName and send requests
 *
 * Each publisher belongs to a single topicName and handles the sending of messages to all the "publishers transports" associated to that topicName.
 *
 * The class is thread-safe
 */
public final class LLZTopicRequester implements ILLZTopicRequester
{
    /** Instance of a Logger class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZTopicRequester.class);

    /** Topic name represented by this requester */
    private final String topicName;

    /** Lock for access to the class */
    private final Object lock = new Object();

    /** True if the publisher has been stopped */
    private boolean stopped = false;

    /** Manager to handle requests, it is used to create new requests */
    private final LLZAsyncSentRequestManager requestManager;

    /** List of associated objects that can send requests for this topic requester */
    private ConcurrentHashMap<Long, RequestSender> requestSendersByTopicId = new ConcurrentHashMap<>();
    
    
    /**
     * Constructor of the class
     *
     * @param topicName Topic that is going to send
     * @param requestManager manager to handle the sent requests
     */
    public LLZTopicRequester(final String topicName, final LLZAsyncSentRequestManager requestManager)
    {
        this.topicName = topicName;
        this.requestManager = requestManager;
    }

    @Override
    public String getTopicName()
    {
        return this.topicName;
    }

    /**
     * Stops the publisher, it wont let any new message to be published
     */
    public void stop()
    {
        synchronized (this.lock)
        {
            this.stopped = true;
        }
    }

    @Override
    public boolean isClosed()
    {
        synchronized (this.lock)
        {
            return this.stopped;
        }
    }
   
    @Override
    public ILLZSentRequest sendRequest(
            final ByteBuffer message,
            final long timeout,
            final ILLZTopicRespListener responseListener,
            final ILLZReqTimeoutListener timeoutListener) throws LLZException
    {
        
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Sending user request on topicName [{}]", this.topicName);
        }

        synchronized (this.lock)
        {
            // Check if not stopped
            if (this.stopped)
            {
                LOGGER.error("Trying to send a request on a topicName that is stopped or destroyed [{}]", this.topicName);
                throw new LLZException("Trying to send a request on an stopped or destroyed topicName: " + this.topicName);
            }

            // Make sure the response listener has been settled
            if (responseListener == null)
            {
                LOGGER.error("Trying to send a request without a response listener");
                throw new LLZException("Cannot send a request without a response listener");
            }
            
            // Create the send request object using the request manager
            final LLZSentRequest sentRequest = this.requestManager.addNewRequest(this.topicName, timeout, responseListener, timeoutListener);

            // Send a request for all the request sender which may have any topicName which matches current regexp
            for (final RequestSender requestSender : this.requestSendersByTopicId.values())
            {
                requestSender.sendRequest(sentRequest, message);
            }

            // Return the sent request object
            return sentRequest;
        }
    }

    public void addRequester(final long topicId, final long responderId, final ILLZTopicRequestSender topicRequestSender)
    {
        final RequestSender requestSender = new RequestSender(topicId, responderId, topicRequestSender);

        this.requestSendersByTopicId.putIfAbsent(topicId, requestSender);
    }

    /**
     * Return true if the requester has been removed
     */
    public boolean removeRequester(final long topicId)
    {
        return this.requestSendersByTopicId.remove(topicId) != null;
    }

    public boolean containsRequester(final long topicId)
    {
        return this.requestSendersByTopicId.containsKey(topicId);
    }

    public Collection<RequestSender> getRequesters()
    {
        return this.requestSendersByTopicId.values();
    }

    public void clearRequesters()
    {
        this.requestSendersByTopicId.clear();
    }

    public class RequestSender
    {
        private final long topicId;
        private final long responderSocketId;
        private ILLZTopicRequestSender sender;

        private RequestSender(long topicId, long responderSocketId, ILLZTopicRequestSender sender)
        {
            this.topicId = topicId;
            this.responderSocketId = responderSocketId;
            this.sender = sender;
        }

        public void sendRequest(final LLZSentRequest requestToSend, final ByteBuffer message) throws LLZException
        {
            this.sender.sendTopicRequest(this.topicId, requestToSend.getRequestId(), message);
        }

        public long getResponderSocketId()
        {
            return responderSocketId;
        }

        public long getTopicId()
        {
            return topicId;
        }
    }
}
