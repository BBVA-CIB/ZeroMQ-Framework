package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.msg.LLZMsgHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.msg.LLZRcvMessage;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The LLZTopicSubscriber is the class that conglomerates all the functionality to subscribe to a topicRegexp.
 *
 * Each subscriber belongs to a single topicRegexp
 *
 * The class is thread-safe
 */
public final class LLZTopicSubscriber implements ILLZTopicSubscriber
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZTopicSubscriber.class);

    /** Topic Name the subscriber belongs to */
    private final String topicName;

    /** Listener for incoming messages */
    private final ILLZTopicSubListener subListener;

    /** Map of all the endpoints by topic Id that belongs to the topic name represented by this topic subscriber */
    private final Map<Long, ILLZAutodiscTopicEndPoint> endPointsByTopicId = new ConcurrentHashMap<>();
    
    /**
     * Constructs a new topic subscriber
     *
     * @param topicName Topic name the subscriber is associated to
     * @param subListener listener for incoming messages on the topicRegexp
     */
    public LLZTopicSubscriber(final String topicName, final ILLZTopicSubListener subListener)
    {
        this.subListener = subListener;
        this.topicName = topicName;
    }

     /** @return topic name which created this subscriber */
    public String getTopicName()
    {
        return this.topicName;
    }

    /** 
     * Creates and process the received user data message
     *
     * @param header
     * @param content content of the received message
     */
    public void onUserDataMessageReceived(LLZMsgHeader header, final ByteBuffer content)
    {
        // First find the topic name
        final ILLZAutodiscTopicEndPoint endPoint = this.endPointsByTopicId.get(header.getTopicUniqueId());

        // This may happen if it has been un-subscribed or the endpoint has been removed from auto-discovery, ignore the message
        if (endPoint == null)
        {
            return;
        }
        
        // Create the messge
        LLZRcvMessage message = new LLZRcvMessage(header, content, endPoint.getTopicName());

        try
        {
            // Send the message to the listener of the user
            this.subListener.onMessageReceived(message);
        }
        catch (final Exception e)
        {
            LOGGER.error("Uncaught exception from user while processing received message", e);
        }
    }

    /**
     * Return the map of endpoints by topic Id that belongs to the topic name represented by this topic subscriber
     */
    public Map<Long, ILLZAutodiscTopicEndPoint> getEndPointsByTopicId()
    {
        return endPointsByTopicId;
    }
}
