package com.bbva.kyof.vega.autodiscovery.client.hazelcast;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.bbva.kyof.vega.autodiscovery.client.LLZAutoDiscTopicEndPoint;
import com.bbva.kyof.vega.exception.LLZException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.hazelcast.core.IMap;

/**
 * Hazelcast Implementation for autodiscovery.
 *
 * THIS CLASS IS NOT THREAD SAFE! THREADING SHOULD BE HANDLED EXTERNALLY
 */
public class LLZHazelcastEndPointPub
{
    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZHazelcastEndPointPub.class);

    /** Hazelcast map which distributes info across all ZMQ instances */
    private IMap<Long, ILLZAutodiscTopicEndPoint> distributedPublisherIdsMap;

    /** Local queue to store which publishers we own and we have to refresh in Hazelcast */
    private Queue<Long> localTopicPublisherIds = new ConcurrentLinkedQueue<>();

    /** True if the object should stop, it is used to stop the refresh of endpoints as soon as possible */
    private volatile boolean shouldStop = false;

    /**
     * Constructor which initializes Hazelcast instances
     * @param distributedPublisherIdsMap
     */
    public LLZHazelcastEndPointPub(final IMap<Long, ILLZAutodiscTopicEndPoint> distributedPublisherIdsMap)
    {
        this.distributedPublisherIdsMap = distributedPublisherIdsMap;
    }

    /**
     * 
     * @param endPointInfo
     * @throws LLZException
     */
    public void registerTopicEndPoint(final LLZAutoDiscTopicEndPoint endPointInfo) throws LLZException
    {
        LOGGER.debug("Adding new publisher information to autodiscovery for topicId [{}], new socket is [{}]",
                endPointInfo.getTopicId(), endPointInfo.getBindAddress());

        // Create the hazelcast objet
        final LLZTopicEndPointDAO hazelcastEndPoint = new LLZTopicEndPointDAO(endPointInfo);

        // Add to hazelcast cache
        this.distributedPublisherIdsMap.set(hazelcastEndPoint.getTopicId(), hazelcastEndPoint);
        
        // Add the topic to the local IDs so we know what to refresh
        this.localTopicPublisherIds.add(hazelcastEndPoint.getTopicId());
    }

    /**
     * 
     * @param socketId
     * @throws LLZException
     */
    public void unregisterTopicEndPoint(final long topicUniqueId) throws LLZException
    {
        LOGGER.debug("Removing information from autodiscovery for topic publisher id [{}]", topicUniqueId);

        // Remove from the cache
        this.distributedPublisherIdsMap.remove(topicUniqueId);

        // Remove the topic to the local IDs so we know what to refresh
        this.localTopicPublisherIds.remove(topicUniqueId);
    }

    /**
     * Keeps alive the end points 
     */
    public void refreshActiveEndpoints()
    {
        for (final long topicPubId : this.localTopicPublisherIds)
        {
            // If marked to stop, return as soon as possible
            if (this.shouldStop)
            {
                return;
            }

            this.distributedPublisherIdsMap.containsKey(topicPubId);
        }
    }

    /**
     * 
     * @throws LLZException
     */
    public void stop() throws LLZException
    {
        this.shouldStop = true;
    }
}
