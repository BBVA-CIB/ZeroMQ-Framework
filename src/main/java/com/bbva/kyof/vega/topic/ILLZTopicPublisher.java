package com.bbva.kyof.vega.topic;

import java.nio.ByteBuffer;

import com.bbva.kyof.vega.exception.LLZException;

/**
 * Interface for a topic publisher with the functionality available for a user
 */
public interface ILLZTopicPublisher
{
    /**
     * This method send to the network to the topic represented by this topic publisher
     *
     * Try to reuse buffers if possible to reduce memory usage!
     *
     * It will send the message contents from position() to limit() on the provided ByteBuffer
     *
     * @param message ByteBuffer containing the binary message to send
     * @throws LLZException if there is any problem in the publication
     */
    void publish (final ByteBuffer message) throws LLZException;

    /** @return the topic associated to this topic publisher */
    String getTopicName();
    
    /** @return topic unique Id associated to this topic publisher */
    long getTopicUniqueId();

    /**
     * @return true if the publisher has been closed.
     *
     * The publishers are closed when the topic is unsubscribed or the context is closed
     */
    boolean isClosed();
}
