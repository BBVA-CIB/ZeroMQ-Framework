package com.bbva.kyof.vega.functional;

import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.ILLZRcvMessage;
import com.bbva.kyof.vega.protocol.ILLZManager;
import com.bbva.kyof.vega.protocol.LLZManager;
import com.bbva.kyof.vega.protocol.LLZManagerParams;
import com.bbva.kyof.vega.topic.ILLZTopicPublisher;

/**
 * Test for the {@link LLZManager} class
 * Created by XE48745 on 15/09/2015.
 */
public class AutoDiscoveryPubSubTest
{
    private static final String CONFIG_FILE = "functional/pubSubTestConfig.xml";
    private static final String APP_NAME = "TestInstance";
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;
    private ILLZManager manager = null;

    @org.junit.BeforeClass
    public static void init()
    {
        MANAGER_CONFIG_FILE = AutoDiscoveryPubSubTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(APP_NAME, MANAGER_CONFIG_FILE).build();
    }

    @Before
    public void beforeTest()
    {
        // Create a new manager instance before each test
        try
        {
            manager = LLZManager.createInstance(MANAGER_PARAMS);
        }
        catch (final LLZException e)
        {
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
        }
    }

    @After
    public void afterTest() throws java.lang.Exception
    {
        // Stop the manager instance after each test and wait a bit to release the sockets
        try
        {
            manager.stop();
            Thread.sleep(3000);
        }
        catch (final LLZException e)
        {
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
        }
    }
  

    @Test
    public void testPubSubOneTopic() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);

        final PubSubTestSubListener listener = new PubSubTestSubListener();
        
        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        
        manager.subscribeToTopic("TOPIC_1", listener);

        // Wait for the connections to be ready
        Thread.sleep(1000);

        // Now send some messages
        this.sendMessage(publisher1, reusableBuffer, "Msg1");
        Thread.sleep(10);
        this.sendMessage(publisher1, reusableBuffer, "Msg2");

        // Wait for the messages to arrive
        Thread.sleep(1000);

        // Check all messages information
        List<ILLZRcvMessage> receivedMessage = listener.getReceivedMessages();
        Assert.assertEquals("Wrong number of messages received", 2, receivedMessage.size());
    }
           
    
    
    @Test
    public void testPubSubMultipleTopics() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);

        final PubSubTestSubListener listener = new PubSubTestSubListener();
        
        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        final ILLZTopicPublisher publisher4 = manager.createPublisher("TOPIC_4");
        
        manager.subscribeToTopic("TOPIC_1", listener);
        manager.subscribeToTopic("TOPIC_2", listener);
        manager.subscribeToTopic("TOPIC_3", listener);
        manager.subscribeToTopic("TOPIC_4", listener);
        

        // Wait for the connections to be ready
        Thread.sleep(1000);

        // Now send some messages
        this.sendMessage(publisher1, reusableBuffer, "Msg1");
        Thread.sleep(10);
        this.sendMessage(publisher2, reusableBuffer, "Msg2");
        Thread.sleep(10);
        this.sendMessage(publisher2, reusableBuffer, "Msg3");
        Thread.sleep(10);
        this.sendMessage(publisher2, reusableBuffer, "Msg4");
        Thread.sleep(10);
        this.sendMessage(publisher3, reusableBuffer, "Msg5");
        Thread.sleep(10);
        this.sendMessage(publisher4, reusableBuffer, "Msg6");

        // Wait for the messages to arrive
        Thread.sleep(1000);

        // Check all messages information
        List<ILLZRcvMessage> receivedMessage = listener.getReceivedMessages();
        Assert.assertEquals("Wrong number of messages received", 6, receivedMessage.size());
    }
    
    
    @Test
    public void testUnsubscribe() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final PubSubTestSubListener listener = new PubSubTestSubListener();
        
        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        
        manager.subscribeToTopic("TOPIC_1", listener);
        manager.subscribeToTopic("TOPIC_2", listener);
        manager.subscribeToTopic("TOPIC_3", listener);

        // Now send some messages
        this.sendMessage(publisher1, reusableBuffer, "Msg1");
        this.sendMessage(publisher2, reusableBuffer, "Msg2");
        this.sendMessage(publisher3, reusableBuffer, "Msg3");

        // Wait for the messages to arrive
        Thread.sleep(1000);

        // Check they have arrive
        Assert.assertEquals(3, listener.getReceivedMessages().size());

        // Now unsubscribe one topic, send messages and check that only two have arrived
        manager.unsubscribeFromTopic("TOPIC_1");
        listener.clear();
        
        this.sendMessage(publisher1, reusableBuffer, "Msg4");
        this.sendMessage(publisher2, reusableBuffer, "Msg5");
        this.sendMessage(publisher3, reusableBuffer, "Msg6");

        // Wait
        Thread.sleep(1000);

        Assert.assertEquals(2, listener.getReceivedMessages().size());
        
    }
       
    private int sendMessage(final ILLZTopicPublisher publisher, final ByteBuffer buffer, final String msg) throws LLUSerializationException, LLZException
    {
        buffer.clear();
        LLUSerializerUtils.STRING.write(msg, buffer);
        buffer.flip();

        final int msgSize = buffer.limit();
        publisher.publish(buffer);

        return msgSize;
    }
}