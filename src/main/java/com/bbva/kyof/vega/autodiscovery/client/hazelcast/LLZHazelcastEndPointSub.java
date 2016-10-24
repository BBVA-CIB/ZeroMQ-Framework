package com.bbva.kyof.vega.autodiscovery.client.hazelcast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPointChangeListener;
import com.bbva.kyof.vega.autodiscovery.client.hazelcast.processors.TopicNameEndPointPredicate;
import com.bbva.kyof.vega.exception.LLZException;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;

/**
 * Hazelcast Implementation for auto-discovery
 *
 * THIS CLASS IS NOT THREAD SAFE! THREADING SHOULD BE HANDLED EXTERNALLY
 */
public class LLZHazelcastEndPointSub
{
    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZHazelcastEndPointSub.class);

    /** Map with all the event handlers registered by topic name */
    private final Map<String, TopicEndPointEventHandler> eventHandlersByTopicName = new HashMap<>();

    /** Hazelcast map which distributes info across all ZMQ instances */
    private final IMap<Long, ILLZAutodiscTopicEndPoint> distributedEndPointsByTopicId;

    /**
     * Constructor which initializes Hazelcast instances
     * 
     * @param distributedEndPointsByTopicId
     */
    public LLZHazelcastEndPointSub(final IMap<Long, ILLZAutodiscTopicEndPoint> distributedEndPointsByTopicId)
    {
        this.distributedEndPointsByTopicId = distributedEndPointsByTopicId;
    }

    /**
     * Subscribes to a topic
     * 
     * @param topicName Topic name
     * @param listener  Listener for changes related with topic endpoints
     */
    public void subscribeToTopic(final String topicName, final ILLZAutodiscTopicEndPointChangeListener listener)
    {
        LOGGER.debug("Adding new listener information to autodiscovery for topic [{}]", topicName);

        if (this.eventHandlersByTopicName.containsKey(topicName))
        {
            LOGGER.error("Already subscribed to changes on topic [{}]", topicName);
            return;
        }

        // Create the event handler
        final TopicEndPointEventHandler eventHandler = new TopicEndPointEventHandler(listener);

        // Store it in the map
        this.eventHandlersByTopicName.put(topicName, eventHandler);

        // Subscribe to notifications on the topic publisher
        this.subscribeToEndPointChanges(topicName, eventHandler);

        // Get the current end points for the topic and notify about them in the listener
        this.findEndPointsForTopic(topicName, eventHandler);

        // Unblock the real time events
        eventHandler.unblockEvents();
    }

    /**
     * 
     * @param topicName
     * @param eventHandler
     */
    private void subscribeToEndPointChanges(final String topicName, final TopicEndPointEventHandler eventHandler)
    {
        final TopicNameEndPointPredicate listenerFilter = new TopicNameEndPointPredicate(topicName);
        final String entryListenerID = this.distributedEndPointsByTopicId.addEntryListener(eventHandler, listenerFilter, true);
        eventHandler.setHazelcastListenerId(entryListenerID);
    }

    /**
     * 
     * @param topicName
     * @param eventHandler
     */
    private void findEndPointsForTopic(final String topicName, final TopicEndPointEventHandler eventHandler)
    {
        // Fire a query to the cache to find all the topic name and end point type
        final Set<Map.Entry<Long, ILLZAutodiscTopicEndPoint>> infoList = this.distributedEndPointsByTopicId.entrySet(new TopicNameEndPointPredicate(topicName));

        // For each result in the query store an action in queue
        for (final Map.Entry<Long, ILLZAutodiscTopicEndPoint> entry : infoList)
        {
            eventHandler.newQueryResult(entry.getValue());
        }
    }

    /**
     * 
     * @param topicName
     * @throws LLZException
     */
    public void unsubscribeFromTopic(final String topicName) throws LLZException
    {
        // Find entry listener for the topic name
        final TopicEndPointEventHandler eventHandler = this.eventHandlersByTopicName.remove(topicName);
        if (eventHandler == null)
        {
            LOGGER.error("Trying to unsubscribe from a non subscribed topic [{}]", topicName);
            throw new LLZException("Trying to unsubscribe from a non subscribed topic, topic name:" + topicName);
        }

        // Remove the notifications listener from hazelcast
        this.distributedEndPointsByTopicId.removeEntryListener(eventHandler.getHazelcastListenerId());
    }

    /**
     * 
     * @author XE52727
     *
     */
    public class TopicEndPointEventHandler implements
            EntryAddedListener<Long, ILLZAutodiscTopicEndPoint>,
            EntryRemovedListener<Long, ILLZAutodiscTopicEndPoint>
    {
        /** Receives notifications about creation/removal of publishers */
        private final ILLZAutodiscTopicEndPointChangeListener eventListener;

        /**  Unique Hazelcast listener identificator  */
        private String hazelcastListenerId;

        /** True if the events from hazelcast should wait */
        private volatile boolean blockEvents = true;

        private Object lock = new Object();
              
        /**
         *  Constructor
         *  
         * @param eventListener
         */
        public TopicEndPointEventHandler(final ILLZAutodiscTopicEndPointChangeListener eventListener)
        {
            this.eventListener = eventListener;
        }
        
        /**
         * 
         * @param listenerId
         */
        public void setHazelcastListenerId(final String listenerId)
        {
            this.hazelcastListenerId = listenerId;
        }

        /**
         * 
         * @return
         */
        public String getHazelcastListenerId()
        {
            return this.hazelcastListenerId;
        }

        @Override
        public void entryAdded(final EntryEvent<Long, ILLZAutodiscTopicEndPoint> entryEvent)
        {
            this.waitUntilEventsUnblock();

            this.eventListener.onEndPointAdded(entryEvent.getValue());
        }

        @Override
        public void entryRemoved(final EntryEvent<Long, ILLZAutodiscTopicEndPoint> entryEvent)
        {
            this.waitUntilEventsUnblock();

            this.eventListener.onEndPointRemoved(entryEvent.getOldValue());
        }

        private void waitUntilEventsUnblock()
        {
            synchronized (this.lock)
            {
                try
                {
                    while (this.blockEvents)
                    {
                        this.lock.wait();
                    }
                }
                catch (final InterruptedException e)
                {
                    LOGGER.error("Interrupted exception while waiting for the real time events to be unblock", e);
                }
            }
        }

        /**
         * Called when there is a new query result
         */
        public void newQueryResult(final ILLZAutodiscTopicEndPoint endPointInfo)
        {
            this.eventListener.onEndPointAdded(endPointInfo);
        }

        /** Changes the flag blockEvents value alternatively */
        public void unblockEvents()
        {
            synchronized (this.lock)
            {
                this.blockEvents = false;
                this.lock.notifyAll();
            }
        }
    }
}