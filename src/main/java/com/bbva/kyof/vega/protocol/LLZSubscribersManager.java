package com.bbva.kyof.vega.protocol;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPointChangeListener;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import com.bbva.kyof.vega.config.general.SubSocketSchema;
import com.bbva.kyof.vega.config.general.SubTopicConfig;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.topic.ILLZTopicSubListener;
import com.bbva.kyof.vega.topic.ILLZTopicSubscriber;
import com.bbva.kyof.vega.topic.LLZTopicSubscriber;

/**
 * Manager to handle Subscribers and reception of messages.
 * <p/>
 * Created by XE48745 on 31/07/2015.
 */
public final class LLZSubscribersManager implements ILLZAutodiscTopicEndPointChangeListener
{
    /** LOGGER Instance   */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZSubscribersManager.class);
    
    /** Stores the topic subscribers by the topic name used to create them. The topic name always comes from the user  */
    private final Map<String, LLZTopicSubscriber> topicSubscriberByTopicName = new HashMap<>();
        
    /** Stores all the subscribers given the publisher ID they are connected to  */
    private final Map<Long, LLZSubscriber> subscriberByPubId = new HashMap<>();
   
    /** Global lock for subscriptions, it will be shared between the hazelcast auto-discovery and the manager to prevent deadlocks */
    private final Object globalLock = new Object();
    
    /** Configuration of the instance  */
    private final LLZInstanceContext instanceContext;

    /** True if the receiver has been stopped  */
    private volatile boolean stopped = false;
    
    /**
     * Constructor of the class
     *
     * @param instanceContext context of the instance
     */
    public LLZSubscribersManager(final LLZInstanceContext instanceContext) throws LLZException
    {
        this.instanceContext = instanceContext;
    }
    
    /**
     * Subscribes a topicName.
     *
     * @param topicName An String with the topic name to subscribe to
     * @param subListener  The Listener where the user wants to receive the messages.
     * @throws LLZException exception thrown if already subscribed or if there is a problem subscribing
     */

    public ILLZTopicSubscriber subscribeToTopic(final String topicName, final ILLZTopicSubListener subListener) throws LLZException
    {
        // Check that there is at a listener
        if (subListener == null)
        {
            LOGGER.error("No listener has been provided subscribing to topic [{}]", topicName);
            throw new LLZException("At least a listener should be provided");
        }

        // Lock on the global lock
        synchronized (this.globalLock)
        {
            // If already stopped launch an error
            this.checkStopped();

            // Try to add it to the map, it will return null if it was added
            if (this.topicSubscriberByTopicName.containsKey(topicName))
            {
                LOGGER.error("Already subscribed to topic [{}]", topicName);
                throw new LLZException("Already subscribed to topic " + topicName);
            }

            // Check if the topic is configured
            if (!this.instanceContext.getInstanceConfig().isSubTopicConfigured(topicName))
            {
                LOGGER.error("The subscriber topic [{}] don't correspond to any configured regular expression in the configuration file of the instance", topicName);
                throw new LLZException("Subscriber topic not found in the instance configuration: " + topicName);
            }

            // Create and add the new topic subscriber
            final LLZTopicSubscriber newTopicSubscriber = new LLZTopicSubscriber(topicName, subListener);
            this.topicSubscriberByTopicName.put(topicName, newTopicSubscriber);

            // Subscribe to auto-discovery in order to get the current status of topics and events of change
            this.instanceContext.getAutodiscovery().subscribeToTopicEndPoints(topicName, LLZAutodiscEndPointType.PUBLISHER, this);

            return newTopicSubscriber;
        }
    }

    /**
     * Unsubscribes from a topicName.
     *
     * @param topicName An String with the topicName to unsubscribe from
     * @throws LLZException exception thrown if not subscribed or if there is a problem unsubscribing
     */
    public void unsubscribeFromTopic(final String topicName) throws LLZException
    {
        // Lock on the global lock
        synchronized (this.globalLock)
        {
            // If already stopped launch an error
            this.checkStopped();

            // Remove the topic subscriber
            final LLZTopicSubscriber topicSubscriber = this.topicSubscriberByTopicName.get(topicName);
            
            if (topicSubscriber == null)
            {
                LOGGER.error("Not subscribed to topic [{}]", topicName);
                throw new LLZException("Not subscribed to topic " + topicName);
            }

            this.unsubscribeFromTopic(topicSubscriber);

            this.topicSubscriberByTopicName.remove(topicName);
        }
    }

    private void unsubscribeFromTopic(final LLZTopicSubscriber topicSubscriber) throws LLZException
    {       
        // Notify Auto Discovery an unregister from events regarding the topicName
        this.instanceContext.getAutodiscovery().unsubscribeFromTopicEndPoints(topicSubscriber.getTopicName(), LLZAutodiscEndPointType.PUBLISHER);

        // For each subscribed endpoint remove it from the subscribers
        for (final ILLZAutodiscTopicEndPoint endPointInfo : topicSubscriber.getEndPointsByTopicId().values())
        {
            this.removeEndPointFromSubscriber(endPointInfo);
        }

        // Clean the internal map just in case
        topicSubscriber.getEndPointsByTopicId().clear();
    }

    /**
     * Stop the manager
     */
    protected void stop() throws LLZException
    {
        // Lock on the global lock
        synchronized (this.globalLock)
        {
            // If already stopped launch an error
            this.checkStopped();

            // Stop and clear the topic subscribers
            for (final LLZTopicSubscriber topicSubscriber : this.topicSubscriberByTopicName.values())
            {
                this.unsubscribeFromTopic(topicSubscriber);
            }

            this.topicSubscriberByTopicName.clear();

            // Set the instance as stopped
            this.stopped = true;
        }
    }

    @Override
    public void onEndPointAdded(final ILLZAutodiscTopicEndPoint autodiscoveryInfo)
    {
        // Lock on the global lock
        synchronized (this.globalLock)
        {
            if (this.stopped)
            {
                return;
            }

            // Check if subscribed to the topic and get the topic subscriber
            final LLZTopicSubscriber topicSubscriber = this.topicSubscriberByTopicName.get(autodiscoveryInfo.getTopicName());
            if (topicSubscriber == null)
            {
                LOGGER.info("New topic end-point notification received but the subscriber is not subscribed anymore. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            // Make sure the end-point is not already added, may happen if there are duplicated events from auto-discovery
            if (topicSubscriber.getEndPointsByTopicId().containsKey(autodiscoveryInfo.getTopicId()))
            {
                LOGGER.info("New topic end-point notification received but the end-point is already registered. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            try
            {
                // Get the subscriber for the publisher info if exists, create in other case
                final LLZSubscriber subscriber = this.getOrCreateSubscriber(autodiscoveryInfo);

                // Subscribe to the topic in the subscriber
                subscriber.subscribeToTopicId(autodiscoveryInfo.getTopicId(), topicSubscriber);

                // If there were no errors add the new endpoint to the list of endpoints for the topic name
                topicSubscriber.getEndPointsByTopicId().put(autodiscoveryInfo.getTopicId(), autodiscoveryInfo);
            }
            catch (final LLZException e)
            {
                LOGGER.error(
                        String.format("Unexpected error subscribing to TopicId [%d], TopicName [%s], PubAddress [%s]. Subscription won't be done",
                        autodiscoveryInfo.getTopicId(),
                        autodiscoveryInfo.getTopicName(),
                        autodiscoveryInfo.getBindAddress()), e);
            }
        }
    }

    @Override
    public void onEndPointRemoved(final ILLZAutodiscTopicEndPoint autodiscoveryInfo)
    {
        // Lock on the global lock
        synchronized (this.globalLock)
        {
            if (this.stopped)
            {
                return;
            }

            //  Get the topic subscriber for the topicName.
            final LLZTopicSubscriber topicSubscriber = this.topicSubscriberByTopicName.get(autodiscoveryInfo.getTopicName());

            if (topicSubscriber == null)
            {
                LOGGER.info("Remove topic end-point notification received but the subscriber is not subscribed anymore. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            // Remove the end-point info from the topic subscriber, if it was not there don't continue
            if (topicSubscriber.getEndPointsByTopicId().remove(autodiscoveryInfo.getTopicId()) == null)
            {
                LOGGER.info("Remove topic end-point notification received but the end-point is not registered. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            try
            {
                // Remove the end-point from the subscriber
                this.removeEndPointFromSubscriber(autodiscoveryInfo);
            }
            catch (final LLZException e)
            {
                LOGGER.error("Unexpected error removing an end-point from a subscriber. " + autodiscoveryInfo, e);
            }
        }
    }

    private void removeEndPointFromSubscriber(final ILLZAutodiscTopicEndPoint autodiscoveryInfo) throws LLZException
    {
        // Find the subscriber(socket) that is connected to the given publisher Id (socket)
        final LLZSubscriber subscriber = this.subscriberByPubId.get(autodiscoveryInfo.getSocketId());

        // It may not exists if there was an exception creating it in the topicPublisherAdded callback
        if (subscriber == null)
        {
            LOGGER.warn("Subscriber for TopicId [{}] was never created, this can happen if there was an error during creation", autodiscoveryInfo.getTopicId());
            return;
        }

        // Unsubscribe from topic in the subscriber and check if there are no more subscriptions for that subscriber
        boolean subscriberIsEmpty = subscriber.unsubscribeFromTopicId(autodiscoveryInfo.getTopicId());

        // If empty remove the subscriber and stop it
        if (subscriberIsEmpty)
        {
            this.subscriberByPubId.remove(autodiscoveryInfo.getSocketId());
            subscriber.stop();
        }
    }

    /**
     * Gets or creates a subscriber
     * 
     * @param autodiscoveryInfo
     * @return the subscriber
     * @throws LLZException
     */
    private LLZSubscriber getOrCreateSubscriber(final ILLZAutodiscTopicEndPoint autodiscoveryInfo) throws LLZException
    {
        // Check if there is already a subscriber(socket) for the event publisher id(socket)
        final LLZSubscriber existingSubscriber = this.subscriberByPubId.get(autodiscoveryInfo.getSocketId());

        // If it already exists, return it if not we need to create it
        if (existingSubscriber != null)
        {
            return existingSubscriber;
        }

        // Check topic name against config.
        final SubTopicConfig subTopicCfg = this.instanceContext.getInstanceConfig().getSubTopicCfg(autodiscoveryInfo.getTopicName());

        // Find the configuration schema for the topic name.
        final SubSocketSchema subSocketSchema = this.instanceContext.getInstanceConfig().getSubSocketSchema(subTopicCfg.getSocketSchema());

        // Create the new subscriber
        final LLZSubscriber newSubscriber = new LLZSubscriber(this.instanceContext, autodiscoveryInfo.getBindAddress(), subSocketSchema);

        // Add to the collection of subscribers
        this.subscriberByPubId.put(autodiscoveryInfo.getSocketId(), newSubscriber);

        return newSubscriber;
    }

    /**
     * Check if the manager has been already stopped
     *
     * @throws LLZException exception thrown if stopped
     */
    private void checkStopped() throws LLZException
    {
        // If already stopped launch an error
        if (this.stopped)
        {
            LOGGER.error("Trying to perform an operation on an stopped receiver manager. InstanceId [{}]",
                    this.instanceContext.getInstanceUniqueId());

            throw new LLZException("Cannot perform an operation on an stopped receiver manager");
        }
    }
}