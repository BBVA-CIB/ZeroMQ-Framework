package com.bbva.kyof.vega.protocol;

import java.util.HashMap;
import java.util.Map;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPointChangeListener;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import com.bbva.kyof.vega.topic.LLZTopicRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.config.general.ReqSocketSchema;
import com.bbva.kyof.vega.config.general.ReqTopicConfig;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.topic.ILLZTopicRequester;

/**
 * The SenderManager encapsulate all the publisher sockets configured and their functionality
 */
public final class LLZRequestersManager implements ILLZAutodiscTopicEndPointChangeListener
{
    /** LOGGER Instance   */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZRequestersManager.class);

    /** Stores the topic requesters by the topic name used to create them. The topic name always comes from the user  */
    private final Map<String, LLZTopicRequester> topicRequestersByTopicName = new HashMap<>();

    /** Stores all the subscribers given the publisher ID they are connected to  */
    private final Map<Long, LLZRequester> requestersByResponderId = new HashMap<>();

    /** Global lock for requester subscriptions, it will be shared between the hazelcast auto-discovery and the manager to prevent deadlocks */
    private final Object globalLock = new Object();

    /** Context of the instance  */
    private final LLZInstanceContext instanceContext;

    /** True if the receiver has been stopped  */
    private volatile boolean stopped = false;

    /**
     * Constructor of the class
     *
     * @param instanceContext context of the instance
     */
    public LLZRequestersManager(final LLZInstanceContext instanceContext) throws LLZException
    {
        this.instanceContext = instanceContext;
    }

    /**
     * Subscribes a topicName.
     *
     * @param topicName An String with the topic name to subscribe to
     * @throws LLZException exception thrown if there is already a requester for that topic
     */

    public ILLZTopicRequester createTopicRequester(final String topicName) throws LLZException
    {
        // Lock on the global lock
        synchronized (this.globalLock)
        {
            // If already stopped launch an error
            this.checkStopped();

            // Try to add it to the map, it will return null if it was added
            if (this.topicRequestersByTopicName.containsKey(topicName))
            {
                LOGGER.error("There is already a requester created for topic [{}]", topicName);
                throw new LLZException("Requester already created for topic " + topicName);
            }

            // Check if the topic is configured as a valid requester topic
            if (!this.instanceContext.getInstanceConfig().isReqTopicConfigured(topicName))
            {
                LOGGER.error("The subscriber topic [{}] don't correspond to any configured regular expression in the configuration file of the instance", topicName);
                throw new LLZException("Subscriber topic not found in the instance configuration: " + topicName);
            }

            // Create and add the new topic requester
            final LLZTopicRequester newTopicRequester = new LLZTopicRequester(topicName, this.instanceContext.getRequestManager());
            this.topicRequestersByTopicName.put(topicName, newTopicRequester);

            // Subscribe to auto-discovery in order to get the current status of topics and events of change
            this.instanceContext.getAutodiscovery().subscribeToTopicEndPoints(topicName, LLZAutodiscEndPointType.RESPONDER, this);

            return newTopicRequester;
        }
    }

    /**
     * Destroys a topic requester
     *
     * @param topicName An String with the topicName of the requester
     * @throws LLZException exception thrown if not subscribed or if there is a problem in the process
     */
    public void destroyTopicRequester(final String topicName) throws LLZException
    {
        // Lock on the global lock
        synchronized (this.globalLock)
        {
            // If already stopped launch an error
            this.checkStopped();

            // Remove the topic subscriber
            final LLZTopicRequester topicRequester = this.topicRequestersByTopicName.get(topicName);

            if (topicRequester == null)
            {
                LOGGER.error("Not requester found for topic [{}]", topicName);
                throw new LLZException("No requester found for topic " + topicName);
            }

            // Destroy the topic requester
            this.destroyTopicRequester(topicRequester);

            // Remove from the map
            this.topicRequestersByTopicName.remove(topicName);
        }
    }

    private void destroyTopicRequester(final LLZTopicRequester topicRequester) throws LLZException
    {
        // Notify Auto Discovery an unregister from events regarding the topicName
        this.instanceContext.getAutodiscovery().unsubscribeFromTopicEndPoints(topicRequester.getTopicName(), LLZAutodiscEndPointType.RESPONDER);

        // For each endpoint associated with the topic requester remove it
        for (final LLZTopicRequester.RequestSender requester : topicRequester.getRequesters())
        {
            this.removeEndPointFromRequester(requester.getTopicId(), requester.getResponderSocketId());
        }

        // Clean the internal map just in case
        topicRequester.clearRequesters();
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
            for (final LLZTopicRequester topicRequester : this.topicRequestersByTopicName.values())
            {
                this.destroyTopicRequester(topicRequester);
            }

            this.topicRequestersByTopicName.clear();

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
            final LLZTopicRequester topicRequester = this.topicRequestersByTopicName.get(autodiscoveryInfo.getTopicName());
            if (topicRequester == null)
            {
                LOGGER.info("New topic end-point notification received but the requester is not subscribed anymore. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            // Make sure the end-point is not already added, may happen if there are duplicated events from auto-discovery
            if (topicRequester.containsRequester(autodiscoveryInfo.getTopicId()))
            {
                LOGGER.info("New topic end-point notification received but the end-point is already registered. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            try
            {
                // Get the requester if already exists for the end-point, create a new one in other case
                final LLZRequester requester = this.getOrCreateRequester(autodiscoveryInfo);

                // Add the end-point to the requester
                requester.addTopicEndPoint(autodiscoveryInfo.getTopicId());

                // Register the requester in the topic requester
                topicRequester.addRequester(autodiscoveryInfo.getTopicId(), autodiscoveryInfo.getSocketId(), requester);
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
            final LLZTopicRequester topicRequester = this.topicRequestersByTopicName.get(autodiscoveryInfo.getTopicName());

            if (topicRequester == null)
            {
                LOGGER.info("Remove topic end-point notification received but the subscriber is not subscribed anymore. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            // Remove the end-point info from the topic subscriber, if it was not there don't continue
            if (!topicRequester.removeRequester(autodiscoveryInfo.getTopicId()))
            {
                LOGGER.info("Remove topic end-point notification received but the end-point is not registered. AutoDiscoveryInfo[{}]", autodiscoveryInfo);
                return;
            }

            try
            {
                // Remove the end-point from the requester
                this.removeEndPointFromRequester(autodiscoveryInfo.getTopicId(), autodiscoveryInfo.getSocketId());
            }
            catch (final LLZException e)
            {
                LOGGER.error("Unexpected error removing an end-point from a subscriber. " + autodiscoveryInfo, e);
            }
        }
    }

    private void removeEndPointFromRequester(final long topicId, final long responderSocketId) throws LLZException
    {
        // Find the subscriber(socket) that is connected to the given publisher Id (socket)
        final LLZRequester requester = this.requestersByResponderId.get(responderSocketId);

        // It may not exists if there was an exception creating it in the topicPublisherAdded callback
        if (requester == null)
        {
            LOGGER.warn("Requester for TopicId [{}] was never created, this can happen if there was an error during creation", topicId);
            return;
        }

        // Remove the end-point information
        final boolean requesterIsEmpty = requester.removeTopicEndPoint(topicId);

        // If no more end-point associated stop and delete the requester
        if (requesterIsEmpty)
        {
            requester.stop();
            this.requestersByResponderId.remove(responderSocketId);
        }
    }

    /**
     * Gets or creates a requester
     *
     * @param autodiscoveryInfo
     * @return the created or existing requester
     * @throws LLZException
     */
    private LLZRequester getOrCreateRequester(final ILLZAutodiscTopicEndPoint autodiscoveryInfo) throws LLZException
    {
        // Check if there is already a subscriber(socket) for the event publisher id(socket)
        final LLZRequester existingRequester = this.requestersByResponderId.get(autodiscoveryInfo.getSocketId());

        // If it already exists, return it if not we need to create it
        if (existingRequester != null)
        {
            return existingRequester;
        }

        // Check topic name against config.
        final ReqTopicConfig reqTopicCfg = this.instanceContext.getInstanceConfig().getReqTopicCfg(autodiscoveryInfo.getTopicName());

        // Find the configuration schema for the topic name.
        final ReqSocketSchema reqSocketSchema = this.instanceContext.getInstanceConfig().getReqSocketSchema(reqTopicCfg.getSocketSchema());

        // Create the requester and return it
        final LLZRequester newRequester = new LLZRequester(this.instanceContext, reqSocketSchema, autodiscoveryInfo.getBindAddress());

        // Add to the collection of requesters
        this.requestersByResponderId.put(autodiscoveryInfo.getSocketId(), newRequester);

        return newRequester;

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
