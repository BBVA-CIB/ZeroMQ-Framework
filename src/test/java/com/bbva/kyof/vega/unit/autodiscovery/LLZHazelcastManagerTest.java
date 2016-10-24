package com.bbva.kyof.vega.unit.autodiscovery;

import org.junit.Test;

import com.bbva.kyof.vega.autodiscovery.client.LLZAutoDiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import com.bbva.kyof.vega.autodiscovery.client.hazelcast.LLZHazelcastManager;
import com.bbva.kyof.vega.config.LLZInstanceLocalConfigReader;
import com.bbva.kyof.vega.functional.AutoDiscoveryPubSubTest;
import com.bbva.kyof.vega.protocol.LLZInstanceContext;
import com.bbva.kyof.vega.protocol.LLZManagerParams;
import com.bbva.kyof.vega.protocol.LLZSubscribersManager;

/**
 * Test LLZAutodiscoveryAbstractImplTest class
 */
public class LLZHazelcastManagerTest
{
    private static final LLZAutodiscEndPointType END_POINT_TYPE = LLZAutodiscEndPointType.PUBLISHER;
    private static final String CONFIG_FILE = "functional/autodiscovery/hazelcastManagerTestConfig.xml";
    private static final String APP_NAME = "TestInstance";
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;

    @org.junit.BeforeClass
    public static void init()
    {
        MANAGER_CONFIG_FILE = AutoDiscoveryPubSubTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(APP_NAME, MANAGER_CONFIG_FILE).build();
    }

   
    @Test
    public void testRegisterAndUnregisterTopicEndPoint() throws Exception
    {
        System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
        
        LLZInstanceContext instanceContext = new LLZInstanceContext(MANAGER_PARAMS);   
              
        // Load and validate the global instance configuration
        final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(MANAGER_PARAMS.getInstanceName(), MANAGER_PARAMS.getConfigurationFile());
        instanceConfigReader.loadAndValidateConfig();

        // Load the configuration into the context
        instanceContext.setInstanceConfiguration(instanceConfigReader.getLoadedConfig());
        
        LLZHazelcastManager autodisc = new LLZHazelcastManager(instanceContext.getInstanceConfig());
        final Long instanceId = autodisc.createUniqueId();
        final Long topicId = autodisc.createUniqueId();
    
        // Create a topic end-point
        LLZAutoDiscTopicEndPoint endPoint = new LLZAutoDiscTopicEndPoint(END_POINT_TYPE, "TOPIC1", 111111L, topicId, instanceId, "");

        // Register the topic end-point in auto-discovery
        autodisc.registerTopicEndPoint(END_POINT_TYPE, endPoint); 

        // Unregister the topic end-point in auto-discovery
        autodisc.unregisterTopicEndPoint(END_POINT_TYPE, topicId);

        // Wait for the async Entryprocessor to delete
        Thread.sleep(5000);
        autodisc.stop();
    }

    
    @Test
    public void testSubscribeAndUnsubscribeToTopicEndPoints() throws Exception
    {
        System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
        LLZInstanceContext instanceContext = new LLZInstanceContext(MANAGER_PARAMS);   

        // Load and validate the global instance configuration
        final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(MANAGER_PARAMS.getInstanceName(), MANAGER_PARAMS.getConfigurationFile());
        instanceConfigReader.loadAndValidateConfig();

        // Load the configuration into the context
        instanceContext.setInstanceConfiguration(instanceConfigReader.getLoadedConfig());

        LLZHazelcastManager autodisc = new LLZHazelcastManager(instanceContext.getInstanceConfig());      
        LLZSubscribersManager listener = new LLZSubscribersManager(instanceContext);

        // Subscribers to events related to an specific topic name. It has to call for each existing publisher
        autodisc.subscribeToTopicEndPoints("TOPIC1", END_POINT_TYPE, listener); 
        
        // Unsubscribes from events related to a topic name
        autodisc.unsubscribeFromTopicEndPoints("TOPIC1", END_POINT_TYPE);

        // Wait for the async Entryprocessor to delete
        Thread.sleep(5000);
        autodisc.stop();
    }

}
