package com.bbva.kyof.vega.protocol;

import java.util.HashMap;
import java.util.Map;

import com.bbva.kyof.vega.autodiscovery.client.LLZAutoDiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import com.bbva.kyof.vega.config.general.RespSocketSchema;
import com.bbva.kyof.vega.config.general.RespTopicConfig;
import com.bbva.kyof.vega.topic.ILLZTopicReqListener;
import com.bbva.kyof.vega.topic.LLZTopicPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.topic.ILLZTopicResponder;
import com.bbva.kyof.vega.topic.LLZTopicResponder;

/**
 * Manager to handle Subscribers and reception of messages.
 * <p/>
 * Created by XE48745 on 31/07/2015.
 */
public final class LLZRespondersManager
{
    /** LOGGER Instance */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZPublishersManager.class);

    /** Stores the topic publisher by topic name */
    private final Map<String, LLZTopicResponder> topicRespondersByTopicName = new HashMap<>();

    /** Store the responder each topic responder belongs to */
    private final Map<LLZTopicResponder, LLZResponder> responderByTopicResponder = new HashMap<>();

    /** Stores the pools of responders */
    private final LLZRespondersPool respondersPools;

    /** Lock for concurrent access to the class */
    private final Object lock = new Object();

    /** Configuration is available in this object */
    private final LLZInstanceContext instanceContext;

    /** True if the receiver has been stopped */
    private boolean stopped = false;

    /**
     * Constructor of the class
     *
     * @param instanceContext context of the instance
     */
    public LLZRespondersManager(final LLZInstanceContext instanceContext) throws LLZException
    {
        this.instanceContext = instanceContext;
        this.respondersPools = new LLZRespondersPool(instanceContext);
    }

    /**
     * Creates the protocol for a publisher
     *
     * @param topicName to publish
     * @param requestListener
     * @return An interface of {@link LLZTopicPublisher} class
     * @throws LLZException
     */
    public ILLZTopicResponder createTopicResponder(final String topicName, ILLZTopicReqListener requestListener) throws LLZException
    {
        // Check that there is at a listener
        if (requestListener == null)
        {
            LOGGER.error("No listener has been provided creating a responder for topic [{}]", topicName);
            throw new LLZException("At least a listener should be provided");
        }

        synchronized (this.lock)
        {
            // If already stopped launch an error
            if (this.stopped)
            {
                LOGGER.error("Trying to create a responder for topic [{}] on an stopped manager", topicName);
                throw new LLZException("Cannot create responders on an stopped manager");
            }

            // Check if not already subscribed
            if (this.topicRespondersByTopicName.containsKey(topicName))
            {
                LOGGER.error("There is a responder already created for topic [{}]", topicName);
                throw new LLZException("There is a responder already created for topic " + topicName);
            }

            // Create a new Publisher (or get one from the pool)
            final LLZResponder responder = this.getOrCreateResponderForTopic(topicName);

            // Create the unique ID for the topic responder
            long topicUniqueId = this.instanceContext.createUniqueId();

            // Create the topic responder
            final LLZTopicResponder topicResponder = new LLZTopicResponder(topicName, topicUniqueId, requestListener);

            // Store created topic responder and responder in the internal maps
            this.topicRespondersByTopicName.put(topicName, topicResponder);
            this.responderByTopicResponder.put(topicResponder, responder);

            // Add the topic responder to the responder
            responder.addTopicResponder(topicUniqueId, topicResponder);

            // Finally register the new created topic responder in auto-discovery
            this.registerTopicResponderInAutodiscovery(responder, topicResponder);

            return topicResponder;
        }
    }


    /**
     * Destroy the publisher for the given topic
     *
     * @param topic the topic the publisher belongs to
     * @throws LLZException exception thrown if the publisher don't exists or cannot be stopped
     */
    public void destroyTopicResponder(final String topic) throws LLZException
    {
        synchronized (this.lock)
        {
            // If already stopped launch an error
            if (this.stopped)
            {
                LOGGER.error("Trying to destroy a responder in topic [{}] on an stopped manager", topic);
                throw new LLZException("Cannot destroy responders on an stopped manager");
            }

            // Get and remove the topic responder
            final LLZTopicResponder topicResponder = this.topicRespondersByTopicName.remove(topic);

            // Make sure it is already subscribed
            if (topicResponder == null)
            {
                LOGGER.error("No responder found for topic [{}]", topic);
                throw new LLZException("There is no responder for topic " + topic);
            }

            this.destroyTopicResponder(topicResponder);
        }
    }

    private void destroyTopicResponder(final LLZTopicResponder topicResponder) throws LLZException
    {
        // Stop the responder
        topicResponder.stop();

        // Remove the topic responder from the responder
        final LLZResponder responder = this.responderByTopicResponder.remove(topicResponder);
        responder.removeTopicResponder(topicResponder.getTopicUniqueId());

        // Unregister the topic responder from auto discovery
        this.unregisterTopicResponderFromAutodiscovery(topicResponder);
    }


    /**
     * Add the topic publisher to the auto-discovery mechanism, it will join together the information of both publisher and topic publisher
     *
     * @param responder the socket responder
     * @param topicResponder the topic responder
     * @throws LLZException
     */
    private void registerTopicResponderInAutodiscovery(final LLZResponder responder, final LLZTopicResponder topicResponder) throws LLZException
    {
        // Get the ID of the application
        final long appId = this.instanceContext.getInstanceUniqueId();

        // Create the object to store in the auto-discovery
        final LLZAutoDiscTopicEndPoint endPointInfo = new LLZAutoDiscTopicEndPoint(
                LLZAutodiscEndPointType.RESPONDER,
                topicResponder.getTopicName(),
                responder.getResponderUniqueId(),
                topicResponder.getTopicUniqueId(),
                appId,
                responder.getResponderFullAddress());

        this.instanceContext.getAutodiscovery().registerTopicEndPoint(endPointInfo.getType(), endPointInfo);

        LOGGER.trace("Topic responder register for auto-discovery [{}]", endPointInfo);
    }

    /**
     * Remove the topic publisher from the auto-discovery mechanism
     *
     * @param topicResponder the topic responder
     * @throws LLZException
     */
    private void unregisterTopicResponderFromAutodiscovery(final LLZTopicResponder topicResponder) throws LLZException
    {
        this.instanceContext.getAutodiscovery().unregisterTopicEndPoint(LLZAutodiscEndPointType.RESPONDER, topicResponder.getTopicUniqueId());

        LOGGER.trace("Topic publisher [{}] unregister from auto-discovery", topicResponder.getTopicUniqueId());
    }

    /**
     * Stop the manager, it will stop all the internal sockets and threads
     *
     * @throws LLZException exception thrown if there is a problem stopping the manager
     */
    public void stop() throws LLZException
    {
        synchronized (this.lock)
        {
            // If already stopped launch an error
            if (this.stopped)
            {
                LOGGER.error("Trying to prepareToStop the responders manager twice");
                throw new LLZException("The responders manager is already stopped");
            }

            // Destroy all the topic responders
            for (final LLZTopicResponder topicResponder : this.topicRespondersByTopicName.values())
            {
                this.destroyTopicResponder(topicResponder);
            }

            // Clean the collection
            this.topicRespondersByTopicName.clear();

            // Stop all the responders in the pool
            this.respondersPools.stopAndCleanAll();

            this.stopped = true;
        }
    }

    /**
     * Get or create a publisher for the given topic.
     *
     * It will look for the transport configuration for the topic. If the configuration is a pool, it will return the next
     * element of the pool or create a new one if the pool has not been filled yet.
     *
     * @param topicName it will be used to look for the topic configuration
     * @return the created or existing publisher
     * @throws LLZException exception thrown if there is any issue
     */
    private LLZResponder getOrCreateResponderForTopic(final String topicName) throws LLZException
    {
        final RespTopicConfig respTopicCfg = this.instanceContext.getInstanceConfig().getRespTopicCfg(topicName);
        final RespSocketSchema respSocketSchema = this.instanceContext.getInstanceConfig().getRespSocketSchema(respTopicCfg.getSocketSchema());

        return this.respondersPools.getOrCreateResponder(respSocketSchema);
    }
}