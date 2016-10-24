package com.bbva.kyof.vega.functional.autodiscovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
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
public class AutodiscoveryTest
{
    private static final String CONFIG_FILE = "functional/autodiscovery/autodiscoveryClient1.xml";
    private static final String APP_NAME = "TestInstance";
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;
    private ILLZManager manager = null;


    @org.junit.BeforeClass public static void init()
    {
        MANAGER_CONFIG_FILE = AutodiscoveryTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(APP_NAME, MANAGER_CONFIG_FILE).build();
    }

    @Before 
    public void beforeTest()
    {
        System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
        // Create a new manager instance before each test
        try
        {
            manager = LLZManager.createInstance(MANAGER_PARAMS);
        }
        catch (final LLZException e)
        {
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error [" + e.getMessage() + "]");
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
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error [" + e.getMessage() + "]");
        }
    }


    @Test 
    public void testPubSubTopicsBefore() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);

        final AutoDiscoveryListener listener = new AutoDiscoveryListener("listener1");
        final AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener2");
        final AutoDiscoveryListener listener3 = new AutoDiscoveryListener("listener3");

        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        final ILLZTopicPublisher publisher4 = manager.createPublisher("TOPIC_4X");
        final ILLZTopicPublisher publisher5 = manager.createPublisher("TOPIC_4Y");
       
        manager.subscribeToTopic("TOPIC_1", listener);
        manager.subscribeToTopic("TOPIC_2", listener);
        manager.subscribeToTopic("TOPIC_3", listener);
        manager.subscribeToTopic("TOPIC_4X", listener2);
        manager.subscribeToTopic("TOPIC_4Y", listener3);
        
        // Wait for the connections to be ready
        Thread.sleep(1000);

        // Now send some messages
        this.sendMessage(publisher1, reusableBuffer, "Msg1");
        this.sendMessage(publisher2, reusableBuffer, "Msg2");
        this.sendMessage(publisher3, reusableBuffer, "Msg3");
        this.sendMessage(publisher4, reusableBuffer, "Msg4");
        this.sendMessage(publisher5, reusableBuffer, "Msg5");
       
        // Wait for the connections to be ready
        Thread.sleep(100);

        assertEquals(3, listener.getReceivedMessages().size());
        assertEquals(1, listener2.getReceivedMessages().size());
        assertEquals(1, listener3.getReceivedMessages().size());
    }

    @Test 
    public void testPubSubTopicsAfter() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
  
        final AutoDiscoveryListener listener = new AutoDiscoveryListener("listener1");
        final AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener2");
        final AutoDiscoveryListener listener3 = new AutoDiscoveryListener("listener3");
        
        manager.subscribeToTopic("TOPIC_1", listener);
        manager.subscribeToTopic("TOPIC_2", listener);
        manager.subscribeToTopic("TOPIC_3", listener);
        manager.subscribeToTopic("TOPIC_4X", listener2);
        manager.subscribeToTopic("TOPIC_4Y", listener3);

        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        final ILLZTopicPublisher publisher4 = manager.createPublisher("TOPIC_4X");
        final ILLZTopicPublisher publisher5 = manager.createPublisher("TOPIC_4Y");
        
        // Wait for the connections to be ready
        Thread.sleep(1000);

        // Now send some messages
        this.sendMessage(publisher1, reusableBuffer, "Msg1");
        this.sendMessage(publisher2, reusableBuffer, "Msg2");
        this.sendMessage(publisher3, reusableBuffer, "Msg3");
        this.sendMessage(publisher4, reusableBuffer, "Msg4");
        this.sendMessage(publisher5, reusableBuffer, "Msg5");
        
        // Wait for the connections to be ready
        Thread.sleep(10000);

        assertEquals(3, listener.getReceivedMessages().size());
        assertEquals(1, listener2.getReceivedMessages().size());
        assertEquals(1, listener3.getReceivedMessages().size());
    }

    @Test 
    public void testPubSubTopicsAddRemove() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
       
        final AutoDiscoveryListener listener = new AutoDiscoveryListener("listener1");
        final AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener2");
        final AutoDiscoveryListener listener3 = new AutoDiscoveryListener("listener3");
        
        manager.subscribeToTopic("TOPIC_1", listener);
        manager.subscribeToTopic("TOPIC_2", listener);
        manager.subscribeToTopic("TOPIC_3", listener);
        manager.subscribeToTopic("TOPIC_4X", listener2);
        manager.subscribeToTopic("TOPIC_4Y", listener3);

        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        final ILLZTopicPublisher publisher4 = manager.createPublisher("TOPIC_4X");
        final ILLZTopicPublisher publisher5 = manager.createPublisher("TOPIC_4Y");
        
        // Wait for the connections to be ready
        manager.destroyPublisher("TOPIC_1");
        Thread.sleep(1000);
        manager.destroyPublisher("TOPIC_2");
        Thread.sleep(1000);

        // Now send some messages
        try
        {
            this.sendMessage(publisher1, reusableBuffer, "Msg1");
            this.sendMessage(publisher2, reusableBuffer, "Msg2");
            assertFalse(true);
        }
        catch (Exception e)
        {
        }
        
        this.sendMessage(publisher3, reusableBuffer, "Msg3");
        this.sendMessage(publisher4, reusableBuffer, "Msg4");
        this.sendMessage(publisher5, reusableBuffer, "Msg5");
        
        // Wait for the connections to be ready
        Thread.sleep(10000);

        assertEquals(1, listener.getReceivedMessages().size());
        assertEquals(1, listener2.getReceivedMessages().size());
        assertEquals(1, listener3.getReceivedMessages().size());
    }

    @Test 
    public void testPubSubTopicsStress() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        
        final AutoDiscoveryListener listener = new AutoDiscoveryListener("listener1");
        final AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener2");
        final AutoDiscoveryListener listener3 = new AutoDiscoveryListener("listener3");

        manager.subscribeToTopic("TOPIC_1", listener);
        manager.subscribeToTopic("TOPIC_2", listener);
        manager.subscribeToTopic("TOPIC_3", listener);
        manager.subscribeToTopic("TOPIC_4X", listener2);
        manager.subscribeToTopic("TOPIC_4Y", listener3);

        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        final ILLZTopicPublisher publisher4 = manager.createPublisher("TOPIC_4X");
        final ILLZTopicPublisher publisher5 = manager.createPublisher("TOPIC_4Y");
        
        // Wait for the connections to be ready
        Thread.sleep(1000);

        // Now send some messages
        this.sendMessage(publisher1, reusableBuffer, "Msg1");
        this.sendMessage(publisher2, reusableBuffer, "Msg2");
        this.sendMessage(publisher3, reusableBuffer, "Msg3");
        this.sendMessage(publisher4, reusableBuffer, "Msg4");
        this.sendMessage(publisher5, reusableBuffer, "Msg5");
       
        // Wait for the connections to be ready
        Thread.sleep(10000);

        assertEquals(3, listener.getReceivedMessages().size());
        assertEquals(1, listener2.getReceivedMessages().size());
        assertEquals(1, listener3.getReceivedMessages().size());
    }

    @Test 
    public void testStress() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final int numElements = 1000;

        List<AutoDiscoveryListener> autoDiscoveryListenersList = new ArrayList<>();
        ILLZTopicPublisher[] publisherArrays = new ILLZTopicPublisher[numElements];
      
        for (int i = 0; i < numElements / 2; ++i)
        {
            publisherArrays[i] = manager.createPublisher("TOPIC_" + i);
        }
        
        for (int i = numElements / 2; i < numElements; ++i)
        {
            publisherArrays[i] = manager.createPublisher("XX_TOPIC_" + i);
        }
             
        for (int i = 0; i < numElements / 2; ++i)
        {
            AutoDiscoveryListener listener1 = new AutoDiscoveryListener("listener" + i );
            manager.subscribeToTopic("TOPIC_" + i, listener1);     
            autoDiscoveryListenersList.add(listener1);
        }
       
        for (int i = numElements / 2; i < numElements; ++i)
        {
            AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener" + i );
            manager.subscribeToTopic("XX_TOPIC_" + i, listener2);
            autoDiscoveryListenersList.add(listener2);
        }
        
        // Wait for the connections to be ready
        Thread.sleep(20000);

        // Now send some messages
        for (int i = 0; i < numElements; ++i)
        {
            this.sendMessage(publisherArrays[i], reusableBuffer, "Msg"+i);
        }
       
        // Wait messages to have arrived
        Thread.sleep(3000);
        
        // Count total received messages on topic [TOPIC_ + i]
        int totalReceivedMsgOnListener1 = 0;  

        for (int i = 0; i < numElements / 2; ++i)
        {
            AutoDiscoveryListener listener1 = autoDiscoveryListenersList.get(i);
            totalReceivedMsgOnListener1 += listener1.getReceivedMessages().size();
            this.checkReceivedTopic(listener1.getReceivedMessages().get(0), "TOPIC_" + i);
        }
                
        // Count total received messages on topic [XX_TOPIC_ + i]
        int totalReceivedMsgOnListener2 = 0;  
        for (int i = numElements / 2; i < numElements; ++i)
        {
            AutoDiscoveryListener listener2 = autoDiscoveryListenersList.get(i);
            totalReceivedMsgOnListener2 += listener2.getReceivedMessages().size();
            this.checkReceivedTopic(listener2.getReceivedMessages().get(0), "XX_TOPIC_" + i);    
        }

        assertEquals(numElements / 2, totalReceivedMsgOnListener1);
        assertEquals(numElements / 2, totalReceivedMsgOnListener2);
    }

    @Test 
    public void testStressPartial() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final AutoDiscoveryListener listener = new AutoDiscoveryListener("listener1");
        final AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener2");

        final int numElements = 1000;
       
        ILLZTopicPublisher[] publisherArrays = new ILLZTopicPublisher[numElements];
        
        for (int i = 0; i < numElements / 2; ++i)
        {
            publisherArrays[i] = manager.createPublisher("TOPIC_" + i);
        }
        
        for (int i = 0; i < numElements / 2; ++i)
        {
            manager.subscribeToTopic("TOPIC_" + i, listener);
        }
        
        for (int i = numElements / 2; i < numElements; ++i)
        {
            manager.subscribeToTopic("XX_TOPIC_" + i, listener2);
        }

        for (int i = numElements / 2; i < numElements; ++i)
        {
            publisherArrays[i] = manager.createPublisher("XX_TOPIC_" + i);
        }
        
        // Wait for the connections to be ready
        Thread.sleep(8000);

        // Now send some messages
        for (int i = 0; i < numElements; ++i)
        {
            this.sendMessage(publisherArrays[i], reusableBuffer, "Msg1");
        }
        
        // Wait for the connections to be ready
        Thread.sleep(800);

        assertEquals(numElements / 2, listener.getReceivedMessages().size());
        assertEquals(numElements / 2, listener2.getReceivedMessages().size());
    }

    private int sendMessage(final ILLZTopicPublisher publisher, final ByteBuffer buffer, final String msg)
            throws LLUSerializationException, LLZException
    {
        buffer.clear();
        LLUSerializerUtils.STRING.write(msg, buffer);
        buffer.flip();

        final int msgSize = buffer.limit();
        publisher.publish(buffer);

        return msgSize;
    }
    
    private void checkReceivedTopic(ILLZRcvMessage rcvMessage, String topic) throws LLUSerializationException
    {
        Assert.assertEquals("Wrong topic received",topic, rcvMessage.getTopicName()) ;
    }
}