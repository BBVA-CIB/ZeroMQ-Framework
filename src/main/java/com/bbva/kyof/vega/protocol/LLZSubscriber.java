package com.bbva.kyof.vega.protocol;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.config.general.SubSocketSchema;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.serialization.LLZMsgHeaderSerializer;
import com.bbva.kyof.vega.sockets.ILLZSubSocketRcvHandler;
import com.bbva.kyof.vega.sockets.LLZSubSocket;
import com.bbva.kyof.vega.topic.LLZTopicSubscriber;

/**
 * The subscriber handles the transport and internal ZMQ socket to receive messages
 */                                                                     
public final class LLZSubscriber implements ILLZSubSocketRcvHandler
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZSubscriber.class);

    /** Subscriber configuration */
    private final SubSocketSchema subscriberConfig;

    /** This Map stores an association between topics unique ids and topic subscribers for incoming messages */
    private final Map<Long, LLZTopicSubscriber> topicSubscriberByTopicId = new ConcurrentHashMap<>();
    
    /** Context of the instance */
    private final LLZInstanceContext instanceContext;
    
    /** ZMQ protocol handler to subscribe to real time message */
    private final LLZSubSocket subscriberSocket;
    
    /** Socket addr which identifies this manager */
    private final String subConnection;

    
    /**
     * Constructor of the class
     * 
     * @param instanceContext Context of the instance
     * @param subConnection subscriber where to connect
     * @param subscriberConfig Configuration of the subscriber
     * @throws LLZException
     */
    public LLZSubscriber(final LLZInstanceContext instanceContext,final String subConnection, final SubSocketSchema subscriberConfig) throws LLZException
    {
        this.subConnection = subConnection;
        this.instanceContext = instanceContext;
        this.subscriberConfig = subscriberConfig;
        
        LOGGER.debug("Creating subscriber manager");

        // Connect and start the subscriber socket
        this.subscriberSocket = new LLZSubSocket(
                instanceContext.getZmqContext(),
                this.subConnection,
                this,
                this.subscriberConfig.getSubRateLimit());
    }
    
    /**
     * Subscribes to a topic.
     *
     * @param topicUniqueId The unique id of the topic+publisher to subscribe
     * @param subEventListener subEventListener listener for topic messages
     * @throws LLZException exception thrown if already subscribed or if there is a problem subscribing
     */
    public void subscribeToTopicId(final long topicUniqueId, final LLZTopicSubscriber subEventListener) throws LLZException
    {
        synchronized (this.topicSubscriberByTopicId)
        {
            // Look for the topic subscriber for that topic ID
            LLZTopicSubscriber listener = this.topicSubscriberByTopicId.get(topicUniqueId);

            // If there is a topic subscriber already. No need to bind the topic, it should already be bonded. 
            if (listener == null)
            {
                // If there is no list, we need to add the new member and tell the socket to bind to the topic.
                this.topicSubscriberByTopicId.put(topicUniqueId, subEventListener);
            }
        }
    }

    /**
     * Unsubscribes from a topic
     *
     * @param uniqueTopicId The topic unique id to unsubscribe from
     *
     * @return if socket has no subscriptions and can be closed
     * @throws LLZException exception thrown if not subscribed or if there is a problem unsubscribing
     */
     public boolean unsubscribeFromTopicId(final long uniqueTopicId) throws LLZException
     {
         this.topicSubscriberByTopicId.remove(uniqueTopicId);
         return this.topicSubscriberByTopicId.isEmpty();
     }


    @Override
    public void onSocketMsgReceived(final ByteBuffer message)
    {
        // Create the received message
        LLZMsgHeader messageHeader;
        try
        {
            messageHeader = LLZMsgHeaderSerializer.deserializeHeader(message);
        }
        catch (final LLZException | LLUSerializationException e)
        {
            LOGGER.error("Error deserializing received message on subscriber " + this.subConnection, e);
            return;
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Message received of type [{}], on topic ID [{}]", messageHeader.getMsgType(), messageHeader.getTopicUniqueId());
        }

        switch (messageHeader.getMsgType())
        {
            case DATA:
                this.processUserDataMessage(messageHeader, message);
                break;
            default:
                LOGGER.warn("Message received of wrong type [{}], expected [{}]", messageHeader.getMsgType(), LLZMsgType.DATA);
                break;
        }
    }

    /**
     * Stop the manager
     */
    public void stop() throws LLZException
    {
        LOGGER.info("Stopping subscriber socket");

        try
        {
            this.subscriberSocket.stop();
        }
        catch (final InterruptedException e)
        {
            LOGGER.error("Thread interrupted while trying to stop the socket", e);
            throw new LLZException("Thread interrupted while trying to stop the socket", e);
        }
    }

    /**
     * Process a received user data message
     *
     * @param header header of the received message
     * @param content content of the received message
     */
    private void processUserDataMessage(final LLZMsgHeader header, final ByteBuffer content)
    {
        // Get the topic listener for the message 
        final LLZTopicSubscriber eventListener = this.topicSubscriberByTopicId.get(header.getTopicUniqueId());

        // If there is no listener ignore the message, it has probably unsubscribed
        if (eventListener != null)
        {
            LOGGER.trace("Sending message received from socket [{}] to its listener", this.getSubConnection());

            // Everything is correct, send header and content to the listener
            eventListener.onUserDataMessageReceived(header, content);
        }
        else
        {
            LOGGER.debug("Received message but no topic listener exists");
        }
    }

    
   /**
     * Returns string/address of ZMQ socket
     * This is the unique ID for this manager (ip:port combination)
     * 
     * @return subscriber socket
     */
    public String getSubConnection()
    {
        return this.subConnection;
    }

}
