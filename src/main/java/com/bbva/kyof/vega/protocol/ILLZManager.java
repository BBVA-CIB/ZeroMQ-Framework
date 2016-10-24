package com.bbva.kyof.vega.protocol;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.topic.ILLZTopicPublisher;
import com.bbva.kyof.vega.topic.ILLZTopicReqListener;
import com.bbva.kyof.vega.topic.ILLZTopicRequester;
import com.bbva.kyof.vega.topic.ILLZTopicResponder;
import com.bbva.kyof.vega.topic.ILLZTopicSubListener;
import com.bbva.kyof.vega.topic.ILLZTopicSubscriber;

/**
 * Interface for the manager. The manager is the main class of the framework
 */
public interface ILLZManager
{
    /**
     * Create a new publisher to send messages into a topic 
     *
     * It is not possible to create 2 publishers for the same topic unless the publisher is destroyed first.
     *
     * Each publisher instance is associated to an unique given topic.
     *
     * @param topic The topic to publish into.
     * @return the created publisher
     * @throws LLZException exception thrown if there is a problem in the subscription or the topic is not configured
     */
    ILLZTopicPublisher createPublisher(final String topic) throws LLZException;

    /**
     * Destroys the publisher for the given topic.
     *
     * @param topic The topic of the publisher to destroy
     * @throws LLZException exception thrown if there is a problem destroying the publisher
     */
    void destroyPublisher(final String topic) throws LLZException;

    /**
     * Create a new requester to send request to responders.
     *
     * It is not possible to create 2 requesters for the same topic unless the requester is destroyed first.
     *
     * Each requester instance is associated to an unique given topic.
     *
     * @param topic The topic to request to.
     * @return the created requester
     * @throws LLZException exception thrown if there is a problem in the subscription or the topic is not configured
     */
    ILLZTopicRequester createRequester(final String topic) throws LLZException;

    /**
     * Destroys the requester for the given topic.
     *
     * If the requester is sending requests it will stop sending them as well.
     *
     * @param topic The topic of the requester to destroy
     * @throws LLZException exception thrown if there is a problem destroying the requester
     */
    void destroyRequester(final String topic) throws LLZException;

    /**
     * Create a new responder to listen to incoming requests on a topic
     *
     * It is not possible to create 2 responders for the same topic unless the responder is destroyed first.
     *
     * Each responder instance is associated to an unique given topic.
     *
     * @param topic The topic to listen for requests from.
     * @param requestListener The listener that will receive the requests
     * @return the created responder
     * @throws LLZException exception thrown if there is a problem creating the responder
     */
    ILLZTopicResponder createResponder(final String topic, final ILLZTopicReqListener requestListener) throws LLZException;

    /**
     * Destroys the responder for the given topic.
     *
     * @param topic The topic of the responder to destroy
     * @throws LLZException exception thrown if there is a problem destroying the responder
     */
    void destroyResponder(final String topic) throws LLZException;
    
    /**
     * Subscribes to the given topic in order to get messages from it.
     * 
     * You cannot subscribe to a topic twice.
     * 
     * @param topicName   Topic name to subscribe to.
     * @param listener    The Listener where the user wants to receive the messages.
     * @return            The topic subscriber to check status 
     * @throws LLZException
     */
    ILLZTopicSubscriber subscribeToTopic(final String topicName, final ILLZTopicSubListener listener) throws LLZException;

    /**
     * Unsubscribe from a topicName.
     *
     * @param topicName Topic name that will be unsubscribed from. Allowed {@link String}
     */
    void unsubscribeFromTopic(final String topicName) throws LLZException;


    /**
     * Stop the manager and all subscribers, publishers, internal sockets and threads associated.
     *
     * It wont return until everything is stopped.
     */
    void stop() throws LLZException;

    /**
     * Returns true if the manager is running
     */
    boolean isRunning();
}
