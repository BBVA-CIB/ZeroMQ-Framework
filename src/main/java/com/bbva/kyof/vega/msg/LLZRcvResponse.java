package com.bbva.kyof.vega.msg;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * This class represents received response
 */
public class LLZRcvResponse extends LLZRcvMessage implements ILLZRcvResponse
{
    
    /**
     * Constructor using the header and the contents of the response
     * 
     * @param header the header of the received response
     * @param content content the contents of the received response
     * @param topicName the topic name the response belongs to
     */
    public LLZRcvResponse(final LLZMsgHeader header, final ByteBuffer content, final String topicName)
    {
        super(header, content, topicName);
    }

    @Override
    public UUID getOriginalRequestId()
    {
        return header.getRequestId();
    }
}
