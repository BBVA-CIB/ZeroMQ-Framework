package com.bbva.kyof.vega.unit.autodiscovery;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

/**
 * Test HazelcastClusterTest class
 */
public class HazelcastClusterTest
{
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testTwoMemberMapSizes()
    {
        // start the first member
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance();
        
        // get the map and put 1000 entries
        Map map1 = h1.getMap("testmap");
        for (int i = 0; i < 1000; i++)
        {
            map1.put(i, "value" + i);
        }
        
        // check the map size
        assertEquals(1000, map1.size());
        
        // start the second member
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance();
        
        // get the same map from the second member
        Map map2 = h2.getMap("testmap");
        
        // check the size of map2
        assertEquals(1000, map2.size());
        
        // check the size of map1 again
        assertEquals(1000, map1.size());
        
        
        
    }

    @Test
    public void testTopic()
    {
        // start two member cluster
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance();
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance();
        String topicName = "TestMessages";
        
        // get a topic from the first member and add a messageListener
        ITopic<String> topic1 = h1.getTopic(topicName);
        final CountDownLatch latch1 = new CountDownLatch(1);
        topic1.addMessageListener(msg ->
        {
            assertEquals("Test1", msg.getMessageObject());
            latch1.countDown();
        });
        
        // get a topic from the second member and add a messageListener
        ITopic<String> topic2 = h2.getTopic(topicName);
        final CountDownLatch latch2 = new CountDownLatch(2);
        topic2.addMessageListener(msg ->
        {
            assertEquals("Test1", msg.getMessageObject());
            latch2.countDown();
        });

        // publish the first message, both should receive this
        topic1.publish("Test1");
        
        // shutdown the first member
        h1.shutdown();
        
        // publish the second message, second member's topic should receive this
        topic2.publish("Test1");
        try
        {
            // assert that the first member's topic got the message
            assertTrue(latch1.await(5, TimeUnit.SECONDS));
        
            // assert that the second members' topic got two messages
            assertTrue(latch2.await(5, TimeUnit.SECONDS));
        }
        catch (InterruptedException ignored)
        {}

    }

    @After
    public void cleanup() throws Exception
    {
        Hazelcast.shutdownAll();
    }
}
