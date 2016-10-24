package com.bbva.kyof.vega.unit.msg;

import static org.junit.Assert.fail;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bbva.kyof.vega.autodiscovery.client.hazelcast.LLZHazelcastManager;
import com.bbva.kyof.vega.config.LLZInstanceLocalConfigReader;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.functional.ReqRespTest;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.msg.LLZRcvMessage;
import com.bbva.kyof.vega.protocol.LLZInstanceContext;
import com.bbva.kyof.vega.protocol.LLZManagerParams;

/**
 * Test the class LLZRcvMessageTest
 */
public class LLZRcvMessageTest
{
    private static final String CONFIG_FILE = "llzManagerTestConfig.xml";
    private static final String INSTANCE_NAME = "managerTestConfig";
    private static String MANAGER_CONFIG_FILE;
    private static LLZManagerParams MANAGER_PARAMS;
    private LLZMsgHeader header;
    private LLZHazelcastManager autodiscovery;
    private long instanceId;
    private long topicUniqueId;
    
       
    @org.junit.BeforeClass
    public static void init()
    {
        MANAGER_CONFIG_FILE = ReqRespTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(INSTANCE_NAME, MANAGER_CONFIG_FILE).build();
    }

   
    @Before
    public void beforeTest()
    {
        try
        {
            System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
            
            LLZInstanceContext instanceContext = new LLZInstanceContext(MANAGER_PARAMS);   
                  
            // Load and validate the global instance configuration
            final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(MANAGER_PARAMS.getInstanceName(), MANAGER_PARAMS.getConfigurationFile());
            instanceConfigReader.loadAndValidateConfig();

            // Load the configuration into the context
            instanceContext.setInstanceConfiguration(instanceConfigReader.getLoadedConfig());
            
            this.autodiscovery = new LLZHazelcastManager(instanceContext.getInstanceConfig());
            this.instanceId = autodiscovery.createUniqueId();
            this.topicUniqueId = autodiscovery.createUniqueId();
            this.header = new LLZMsgHeader(LLZMsgType.DATA_RESP, this.topicUniqueId, this.instanceId,"2.0");
        
        }
        catch (final LLZException e)
        {
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
        }
        
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
        }

    }

    @After
    public void afterTest() throws LLZException {
        autodiscovery.stop();
    }

    @Test
    public void testGettersSetters() throws LLZException
    {
        // Create the contents
        final ByteBuffer msgContents = ByteBuffer.allocate(128);
        
        // Create the message
        final LLZRcvMessage rvcMessage = new LLZRcvMessage(this.header, msgContents, "TOPIC_1");

        Assert.assertEquals(LLZMsgType.DATA_RESP, rvcMessage.getMessageType());
        Assert.assertEquals("2.0", rvcMessage.getVersion());
        Assert.assertEquals("TOPIC_1", rvcMessage.getTopicName());
        Assert.assertSame(this.topicUniqueId, rvcMessage.getTopicId());
        Assert.assertSame(this.instanceId, rvcMessage.getInstanceId());
        Assert.assertEquals(msgContents, rvcMessage.getMessageContent());

        rvcMessage.setMessageContents(null);
        Assert.assertEquals(null, rvcMessage.getMessageContent());
    }

    @Test
    public void testPromote() throws LLZException
    {
        // Create the contents
        final ByteBuffer msgContents = ByteBuffer.allocate(128);

        // Add something
        msgContents.putLong(1111111L);
        msgContents.putLong(2222222L);
        msgContents.putLong(3333333L);
        msgContents.flip();

        // Create the message
        final LLZRcvMessage rvcMessage = new LLZRcvMessage(this.header, msgContents, "TOPIC_1");

        // Now promote the message with non direct buffer
        rvcMessage.promote();
        Assert.assertNotSame(rvcMessage.getMessageContent(), msgContents);
        Assert.assertFalse(rvcMessage.getMessageContent().isDirect());
        this.checkBufferContents(rvcMessage.getMessageContent());

        // Promote with provided buffer
        final ByteBuffer originalBuffer = rvcMessage.getMessageContent();
        final ByteBuffer replaceBuffer = ByteBuffer.allocate(128);
        rvcMessage.promote(replaceBuffer);

        Assert.assertEquals(rvcMessage.getMessageContent(), originalBuffer);
        Assert.assertTrue(rvcMessage.getMessageContent() == replaceBuffer);
        Assert.assertFalse(rvcMessage.getMessageContent().isDirect());
        this.checkBufferContents(rvcMessage.getMessageContent());

        // Finally provide a buffer too small to produce an exception
        try
        {
            rvcMessage.promote(ByteBuffer.allocate(1));
        }
        catch (final BufferOverflowException e)
        {
            return;
        }

        Assert.assertTrue("An exception should has been thrown", false);
    }

    private void checkBufferContents(final ByteBuffer target)
    {
        Assert.assertEquals(target.getLong(), 1111111L);
        Assert.assertEquals(target.getLong(), 2222222L);
        Assert.assertEquals(target.getLong(), 3333333L);

        target.flip();
    }
}