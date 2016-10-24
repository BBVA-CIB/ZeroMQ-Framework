package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.exception.LLZException;

import java.nio.ByteBuffer;

/**
 * Interface to implement in order to send user topic messages
 */
public interface ILLZTopicMsgPublisher
{
    /**
     * Send a message for the given topic with the provided contents
     *
     * @param topic the topic the message belong to
     * @param topicPublisherUniqueId unique Id of the topic publisher that is calling the method
     * @param message the message to send
     * @throws LLZException exception thrown if there is a problem sending the message
     */
    void sendMessage(final String topic, final long topicPublisherUniqueId, final ByteBuffer message) throws LLZException;
}
