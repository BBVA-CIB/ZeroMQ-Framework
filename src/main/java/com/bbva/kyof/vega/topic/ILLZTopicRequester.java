package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.ILLZReqTimeoutListener;
import com.bbva.kyof.vega.msg.ILLZSentRequest;
import com.bbva.kyof.vega.msg.ILLZTopicRespListener;

import java.nio.ByteBuffer;

/**
 * Interface for a topic requester with the functionality available for a user
 */
public interface ILLZTopicRequester
{   
    
    /**
     * Send a request on the topic. A unique identifier should be provided to identify the response.
     *
     * It will send the message contents from position() to limit() on the provided ByteBuffer.
     *
     * Try to reuse buffers if possible to reduce memory allocation.
     * 
     * @param message The request message to send
     * @param timeout
     * @param responseListener
     * @param timeoutListener
     * 
     * @return an object that represent the sent request, containing the request ID and other usefull information
     * @throws LLZException LLZException if there is any problem in the publication
     */
    ILLZSentRequest sendRequest(final ByteBuffer message,
                                final long timeout,
                                final ILLZTopicRespListener responseListener,
                                final ILLZReqTimeoutListener timeoutListener) throws LLZException;

    /** @return the topic associated to this publisher */
    String getTopicName();

    /**
     * @return true if the publisher has been closed.
     *
     * The publishers are closed when the topic is unsubscribed or the context is closed
     */
    boolean isClosed();
}
