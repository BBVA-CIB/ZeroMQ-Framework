package com.bbva.kyof.vega.msg;

import com.bbva.kyof.vega.exception.LLZException;
import org.zeromq.ZFrame;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Method to implement in order to respond requests
 */
public interface ILLZRcvReqResponder
{
    /**
     * Send a response for a received request
     *
     * @param topicId the topic id of the response
     * @param topicName name of the topic
     * @param requestId the original request Id
     * @param responseContent the contents of the response
     * @param responseAddress the ZMQ address for the response    @throws LLZException exception thrown if there is any problem sending the response
     */
    void sendReqResponse(final long topicId,
                         final String topicName,
                         final UUID requestId,
                         final ByteBuffer responseContent,
                         final ZFrame responseAddress) throws LLZException;
}
