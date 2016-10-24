package com.bbva.kyof.vega.topic;

/**
 * Interface for a topic responder with the functionality available for a user
 */
public interface ILLZTopicResponder
{
    /** @return the topic associated to this publisher */
    String getTopicName();

    /**
     * @return true if the responder has been closed.
     *
     * The responders are closed when the topic is unsubscribed or the context is closed
     */
    boolean isClosed();
}
