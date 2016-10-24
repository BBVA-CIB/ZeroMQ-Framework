package com.bbva.kyof.vega.autodiscovery.client;


import com.bbva.kyof.vega.exception.LLZException;

/**
 * Interface to be implemented by autodiscovery technologies
 * Implementations have to be thread safe
 * <p/>
 * This interface is designed to be used internally, not by users
 * If everything @ this interface is implemented with any technology (Hazelcast, coherence, sockets :) )
 * autodiscovery should work
 */
public interface ILLZAutodisc
{
    /**
     * Stops the instance/underlying storage,
     * only for the local node
     */
    void stop() throws LLZException;

    /**
     * Returns the unique id across all the nodes in the "cluster"
     * Note: This method only needs to return the instanceConfigID for the name of the nodes
     * our JVM is publishing
     *
     * @return
     */
    long createUniqueId() throws LLZException;

    /**
     * Register a topic end-point in auto-discovery
     * 
     * @param endPointType type of the endpoint to register
     * @param endPointInfo endPointInfo endpoint information
     * @throws LLZException
     */
    void registerTopicEndPoint(final LLZAutodiscEndPointType endPointType, final LLZAutoDiscTopicEndPoint endPointInfo) throws LLZException;
 
    /**
     * Unregister a topic end-point in auto-discovery
     * 
     * @param endPointType type of the endpoint to unregister
     * @param topicUniqueId the unique id of the topic of the end point
     * @throws LLZException
     */
    void unregisterTopicEndPoint(final LLZAutodiscEndPointType endPointType, final  long topicUniqueId) throws LLZException;

    
    /**
     * Subscribers to events related to an specific topic name. It has to call for each existing publisher
     * 
     * @param topicName topic name
     * @param endPointType type of the endpoint to subscribe
     * @param listener listener that will receive the events for the topic name and type
     * @throws LLZException if timeout is reached and no topic appears with that name
     */
    void subscribeToTopicEndPoints(final String topicName, final LLZAutodiscEndPointType endPointType, final ILLZAutodiscTopicEndPointChangeListener listener) throws LLZException;

    /**
     * Unsubscribes from events related to a topic name
     *
     * @param topicName topic Regexp
     * @param endPointType type of the endpoint to unsubscribe from
     * @throws LLZException if timeout is reached and no topic appears with that name
     */
    void unsubscribeFromTopicEndPoints(final String topicName, final LLZAutodiscEndPointType endPointType) throws LLZException;
}
