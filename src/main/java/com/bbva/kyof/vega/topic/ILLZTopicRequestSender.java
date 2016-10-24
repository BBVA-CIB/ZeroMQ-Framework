package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.exception.LLZException;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Method to implement in order to send requests associated to a topic
 */
public interface ILLZTopicRequestSender
{
    /**
     * Send a topic request given the topic, the unique ID of the request and the contents of the message
     *
     * @param topicId the topicId for the request to send
     * @param reqId the unique identifier for the request
     * @param messageContents the contents of the message to send
     * @throws LLZException exception thrown if there is any problem sending the request
     */
    void sendTopicRequest(final long topicId, final UUID reqId, final ByteBuffer messageContents) throws LLZException;
}
