package com.bbva.kyof.vega.msg;

import com.bbva.kyof.vega.exception.LLZException;
import org.zeromq.ZFrame;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * This class represents received request
 */
public class LLZRcvRequest extends LLZRcvMessage implements ILLZRcvRequest
{
    /** Object that will respond the request when the send response method is called */
    private final ILLZRcvReqResponder requestResponder;

    /** The ZMQ address the response should be sent to */
    private final ZFrame responseAddress;

    /**
     * Construct a new received request
     *
     * @param header the header of the request
     * @param content the content of the request message
     * @param responseAddress the address the response should be sent to
     * @param requestResponder object that will respond the request when the send response method is called
     */
    public LLZRcvRequest(final LLZMsgHeader header,
                         final ByteBuffer content,
                         final ZFrame responseAddress,
                         final ILLZRcvReqResponder requestResponder,
                         final String topicName)
    {
        super(header, content, topicName);
        this.responseAddress = responseAddress;
        this.requestResponder = requestResponder;
    }

    /** Returns the unique ID of the received request */
    public UUID getRequestId()
    {
        return header.getRequestId();
    }

    @Override
    public void sendResponse(final ByteBuffer responseContent) throws LLZException
    {
        this.requestResponder.sendReqResponse(header.getTopicUniqueId(), this.getTopicName(), this.getRequestId(), responseContent, this.responseAddress);
    }
}
