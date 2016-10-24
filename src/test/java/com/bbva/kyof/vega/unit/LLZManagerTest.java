package com.bbva.kyof.vega.unit;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.functional.PubSubTestSubListener;
import com.bbva.kyof.vega.protocol.ILLZManager;
import com.bbva.kyof.vega.protocol.LLZManager;
import com.bbva.kyof.vega.protocol.LLZManagerParams;
import com.bbva.kyof.vega.topic.ILLZTopicPublisher;

/**
 * Test for the {@link LLZManager} class
 * Created by XE48745 on 15/09/2015.
 */
public class LLZManagerTest
{
    private static final String CONFIG_FILE = "llzManagerTestConfig.xml";
    private static final String INSTANCE_NAME = "managerTestConfig";
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;
    private ILLZManager manager = null;

    @org.junit.BeforeClass
    public static void init()
    {
        MANAGER_CONFIG_FILE = LLZManagerTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(INSTANCE_NAME, MANAGER_CONFIG_FILE).build();
    }

    @Before
    public void beforeTest() throws java.lang.Exception
    {
        // Create a new manager instance before each test
        manager = LLZManager.createInstance(MANAGER_PARAMS);
    }

    @After
    public void afterTest() throws java.lang.Exception
    {
        // Stop the manager instance after each test and wait a bit to release the sockets
        if (manager != null)
        {
            if (manager.isRunning())
            {
                manager.stop();
            }

            manager = null;
        }

        Thread.sleep(3000);
    }

    @Test(expected = LLZException.class)
    public void testCreatePublisherFail() throws LLZException
    {
        manager.createPublisher("WRONG_TOPIC");
    }

    @Test(expected = LLZException.class)
    public void testCreatePublisherTwice() throws LLZException
    {
        manager.createPublisher("TOPIC_1");
        manager.createPublisher("TOPIC_1");
    }

    //Subscribers cant fail, you just start to listen for new publishers if no publisher exists
    @Test(expected = LLZException.class)
    public void testSubscribeToTopicFail() throws LLZException
    {
        manager.subscribeToTopic("WRONG_TOPIC", new PubSubTestSubListener());
    }

    @Test(expected = LLZException.class)
    public void testSubscribeToTopicTwice() throws LLZException
    {
        manager.subscribeToTopic("TOPIC_1", new PubSubTestSubListener());
        manager.subscribeToTopic("TOPIC_1", new PubSubTestSubListener());
    }

    @Test(expected = LLZException.class)
    public void testUnsubscribeFromTopicFail() throws LLZException
    {
        manager.unsubscribeFromTopic("TOPIC_1");
    }

    @Test(expected = LLZException.class)
    public void testSendMessageAfterClosingPublisher() throws LLZException
    {
        ILLZTopicPublisher publisher = manager.createPublisher("TOPIC_1");
        manager.stop();
        manager = null;
        publisher.publish(ByteBuffer.allocate(128));
    }

    @Test
    public void testCallManagerMethodsAfterClosing() throws LLZException
    {
        manager.createPublisher("TOPIC_1");
        manager.stop();

        try
        {
            manager.stop();
            Assert.fail("Trying to stop it twice should have failed");
        }
        catch (LLZException e){}
        try
        {
            manager.subscribeToTopic("TOPIC_1", new PubSubTestSubListener());
            Assert.fail("Subscribe after stopping should have failed");
        }
        catch (LLZException e){}
        try
        {
            manager.unsubscribeFromTopic("TOPIC_1");
            Assert.fail("Unsubscribe after stopping should have failed");
        }
        catch (LLZException e){}
        try
        {
            manager.createPublisher("TOPIC_1");
            Assert.fail("Creating publisher after stopping should have failed");
        }
        catch (LLZException e){}

        manager = null;
    }

    
    @Test (expected = LLZException.class)
    public void doubleInstanceOverSamePorts() throws LLZException, InterruptedException
    {
        //Config only allows for one port on the socket Pub schema
        manager.createPublisher("TOPIC_1");
        
        // Create a new manager instance with the same config, should cause a port problem
        final ILLZManager secondManager = LLZManager.createInstance(MANAGER_PARAMS);
        secondManager.createPublisher("TOPIC_1");
        
        this.stopManager(secondManager);
        
    }

    @Test(expected = LLZException.class)
    public void createInstanceFail() throws LLZException
    {
        final LLZManagerParams params = new LLZManagerParams.Builder(INSTANCE_NAME, "WrongFileName").build();
        manager = LLZManager.createInstance(params);
    }
    
    
    public void stopManager(final ILLZManager manager) throws LLZException, InterruptedException 
    {
        // Stop the manager instance after each test and wait a bit to release the sockets
        if (manager != null && manager.isRunning())
        {
            manager.stop();
        }

        Thread.sleep(3000);
    }
    
    // TODO, perform a multithread intensive test
}