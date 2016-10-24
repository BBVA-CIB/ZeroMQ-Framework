package com.bbva.kyof.vega.unit.msg;

import java.nio.ByteBuffer;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.msg.LLZRcvResponse;

/**
 * Test the LLZRcvResponse class
 */
public class LLZRcvResponseTest
{
    private UUID uuidTest;
    private LLZMsgHeader header;

    @Before
    public void beforeTest()
    {
        this.uuidTest = UUID.randomUUID();
        this.header = new LLZMsgHeader(LLZMsgType.DATA_RESP, 2L, 11111, "2.0");
    }

    @Test
    public void testGettersSetters()
    {
        // Create the contents
        final ByteBuffer msgContents = ByteBuffer.allocate(128);  

        // Create the message
        final LLZRcvResponse response = new LLZRcvResponse(this.header, msgContents, "TOPIC_1");

        Assert.assertEquals(response.getOriginalRequestId(), null);
        Assert.assertEquals(response.getTopicName(), "TOPIC_1");
        Assert.assertEquals(LLZMsgType.DATA_RESP, response.getMessageType());
        Assert.assertEquals("2.0", response.getVersion());
        Assert.assertSame(2L, response.getTopicId());
        Assert.assertEquals(msgContents, response.getMessageContent());
        
        header.setRequestId(uuidTest);

        Assert.assertEquals(response.getOriginalRequestId(), this.uuidTest);
 
       
    }
}