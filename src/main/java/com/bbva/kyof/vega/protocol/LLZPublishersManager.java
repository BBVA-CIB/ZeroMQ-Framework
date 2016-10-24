package com.bbva.kyof.vega.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bbva.kyof.vega.autodiscovery.client.LLZAutoDiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.config.general.PubSocketSchema;
import com.bbva.kyof.vega.config.general.PubTopicConfig;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.topic.ILLZTopicPublisher;
import com.bbva.kyof.vega.topic.LLZTopicPublisher;

/**
 * The Publisher Manager encapsulate all the publisher sockets configured and their functionality
 */
public final class LLZPublishersManager
{
    /** LOGGER Instance */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZPublishersManager.class);

    /** Stores the topic publisher by topic name */
    private final Map<String, LLZTopicPublisher> topicPublishersByTopicName = new ConcurrentHashMap<>();

 	/** Publishers pools manager */
    private final LLZPublishersPools publishersPools;

    /** Lock for concurrent access to the class */
    private final Object lock = new Object();

    /** Configuration is available in this object */
    private final LLZInstanceContext instanceContext;
    
    /** True if the receiver has been stopped */
    private boolean stopped = false;


    /**
     * Constructor
     *
     * @param instanceContext the context of the manager instance
     */
    public LLZPublishersManager(final LLZInstanceContext instanceContext) throws LLZException
    {
        this.instanceContext = instanceContext;
        this.publishersPools = new LLZPublishersPools(instanceContext);
    }


    /**
     * Creates the protocol for a publisher
     * 
     * @param topic to publish
     * @return An interface of {@link LLZTopicPublisher} class
     * @throws LLZException
     */
    public ILLZTopicPublisher createTopicPublisher(final String topic) throws LLZException
    {
        synchronized (this.lock)
        {
            // If already stopped launch an error
            if (this.stopped)
            {
                LOGGER.error("Trying to create a publisher for topic [{}] on an stopped manager", topic);
                throw new LLZException("Cannot create publishers on an stopped manager");
            }

            // Check if not already subscribed
            if (this.topicPublishersByTopicName.containsKey(topic))
            {
                LOGGER.error("Already subscribed to topic [{}]", topic);
                throw new LLZException("Already subscribed to topic " + topic);
            }

            // Create a new Publisher (or get one from the pool)
            final LLZPublisher publisher = this.getOrCreatePublisherForTopic(topic);

            // Unique ID for the topic publisher
            long topicUniqueId = this.instanceContext.createUniqueId();

            // Create the topic publisher
            final LLZTopicPublisher topicPublisher = new LLZTopicPublisher(topic, topicUniqueId, publisher);

            // Store the topic publisher in a map
            this.topicPublishersByTopicName.put(topic, topicPublisher);

            // Finally register the new created topic publisher in auto-discovery
            this.registerTopicPublisherInAutodiscovery(publisher, topicPublisher);

            return topicPublisher;
        }
    }


    /**
     * Destroy the publisher for the given topic
     *
     * @param topic the topic the publisher belongs to
     * @throws LLZException exception thrown if the publisher don't exists or cannot be stopped
     */
    public void destroyTopicPublisher(final String topic) throws LLZException
    {
        synchronized (this.lock)
        {
            // If already stopped launch an error
            if (this.stopped)
            {
                LOGGER.error("Trying to destroy a publisher in topic [{}] on an stopped manager", topic);
                throw new LLZException("Cannot destroy publishers on an stopped manager");
            }

            // Get and remove the topic publisher
            final LLZTopicPublisher topicPublisher = this.topicPublishersByTopicName.remove(topic);

            // Make sure it is already subscribed
            if (topicPublisher == null)
            {
                LOGGER.error("No publisher found for topic [{}]", topic);
                throw new LLZException("There is no publisher for topic " + topic);
            }
            
            topicPublisher.stop();

            // Unregister the topic publisher from auto discovery
            this.unregisterTopicPublisherFromAutoDiscovery(topicPublisher);
        }
    }


    /**
     * Add the topic publisher to the auto-discovery mechanism, it will join together the information of both publisher and topic publisher
     *
     * @param publisher the socket publisher
     * @param topicPublisher the topic publisher
     * @throws LLZException 
     */
    private void registerTopicPublisherInAutodiscovery(final LLZPublisher publisher, final LLZTopicPublisher topicPublisher) throws LLZException
    {
        // Get the ID of the application
        final long appId = this.instanceContext.getInstanceUniqueId();

        // Create the object to store in the auto-discovery
        final LLZAutoDiscTopicEndPoint endPointInfo = new LLZAutoDiscTopicEndPoint(
                LLZAutodiscEndPointType.PUBLISHER,
                topicPublisher.getTopicName(),
                publisher.getPublisherUniqueId(),
                topicPublisher.getTopicUniqueId(),
                appId,
                publisher.getPublisherFullAddress());

        this.instanceContext.getAutodiscovery().registerTopicEndPoint(endPointInfo.getType(), endPointInfo);

        
        LOGGER.trace("Topic publisher register for auto-discovery [{}]", endPointInfo);
    }

    /**
     * Remove the topic publisher from the auto-discovery mechanism
     *
     * @param topicPublisher the topic publisher
     * @throws LLZException 
     */
    private void unregisterTopicPublisherFromAutoDiscovery(final LLZTopicPublisher topicPublisher) throws LLZException
    {
        this.instanceContext.getAutodiscovery().unregisterTopicEndPoint(LLZAutodiscEndPointType.PUBLISHER, topicPublisher.getTopicUniqueId());

        LOGGER.trace("Topic publisher [{}] unregister from auto-discovery", topicPublisher.getTopicUniqueId());
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
                LOGGER.error("Trying to prepareToStop the sender manager twice");
                throw new LLZException("The sender manager is already stopped");
            }

            // Destroy all the topic publishers
            for (final String topicName : this.topicPublishersByTopicName.keySet())
            {
                this.destroyTopicPublisher(topicName);
            }

            // Clean collection
            this.topicPublishersByTopicName.clear();

            // Stop and clean the pools of publishers
            this.publishersPools.stopAndCleanAll();

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
    private LLZPublisher getOrCreatePublisherForTopic(final String topicName) throws LLZException
    {
        final PubTopicConfig pubTopicCfg = this.instanceContext.getInstanceConfig().getPubTopicCfg(topicName);
        final PubSocketSchema pubSocketSchema = this.instanceContext.getInstanceConfig().getPubSocketSchema(pubTopicCfg.getSocketSchema());

        return this.publishersPools.getOrCreatePublisher(pubSocketSchema);
    }
}
