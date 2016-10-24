package com.bbva.kyof.vega.unit.msg;

import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;

import junit.framework.Assert;

import org.junit.Test;

import java.util.UUID;

/**
 * Test the LLZMsgHeader class
 */
public class LLZMsgHeaderTest
{
    private static final long APP_ID = 238472897463L;
    private static final long TOPIC_ID = 2L;
    private static final String FRAMEWORK_VERSION = "2.0";
    
    
    @Test
    public void testGettersSetters()
    {
        final UUID uuidTest = UUID.randomUUID();

        LLZMsgHeader header = new LLZMsgHeader();
        header.setMsgType(LLZMsgType.DATA);
        header.setTopicUniqueId(TOPIC_ID);
        header.setVersion(FRAMEWORK_VERSION);
        header.setInstanceId(APP_ID);

        Assert.assertEquals(header.getVersion(), FRAMEWORK_VERSION);
        Assert.assertEquals(header.getTopicUniqueId().longValue(), TOPIC_ID);
        Assert.assertEquals(header.getMsgType(), LLZMsgType.DATA);
        Assert.assertEquals(header.getInstanceId(), APP_ID);
        Assert.assertEquals(header.getRequestId(), null);

        // Set the request
        header.setRequestId(uuidTest);
        Assert.assertEquals(header.getRequestId(), uuidTest);
    }

    @Test
    public void testConstructor()
    {
        final UUID uuidTest = UUID.randomUUID();
        
        LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA, TOPIC_ID, APP_ID, FRAMEWORK_VERSION);


        Assert.assertEquals(header.getVersion(), FRAMEWORK_VERSION);
        Assert.assertEquals(header.getTopicUniqueId().longValue(),TOPIC_ID);
        Assert.assertEquals(header.getMsgType(), LLZMsgType.DATA);
        Assert.assertEquals(header.getInstanceId(), APP_ID);
        Assert.assertEquals(header.getRequestId(), null);

        // Set the request ID
        header.setRequestId(uuidTest);
        Assert.assertEquals(header.getRequestId(), uuidTest);
    }
}