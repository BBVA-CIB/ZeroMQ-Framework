package com.bbva.kyof.vega.unit.config.general;


import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.bbva.kyof.vega.config.general.AutoDiscoveryConfig;
import com.bbva.kyof.vega.config.general.GlobalConfiguration;
import com.bbva.kyof.vega.config.general.InstanceConfig;
import com.bbva.kyof.vega.config.general.NodeType;
import com.bbva.kyof.vega.config.general.PubSocketSchema;
import com.bbva.kyof.vega.config.general.PubTopicConfig;
import com.bbva.kyof.vega.config.general.SubSocketSchema;
import com.bbva.kyof.vega.config.general.SubTopicConfig;
import com.bbva.kyof.vega.config.general.TransportMediaType;

/**
 * Created by cnebrera on 26/10/15.
 */
public class ConfigurationTest
{
    @Test
    public void testGetInstance() throws Exception
    {     
        // Create the autodiscovery configuration
        AutoDiscoveryConfig autodiscoConf = this.createAutoDiscoConfig();
                
        // Create the publisher
        PubSocketSchema pub1 = createPublisher();

        // Create the subscriber
        SubSocketSchema sub1 = new SubSocketSchema();
        sub1.setName("sub1");
        sub1.setSubRateLimit(10L);

        // Create the publisher topic
        PubTopicConfig pubTopicConfig= new PubTopicConfig();
        pubTopicConfig.setSocketSchema("default");
        pubTopicConfig.setPattern("TestPubTopic");
        
        // Create the subscriber topic
        SubTopicConfig subTopicConfig= new SubTopicConfig();
        subTopicConfig.setSocketSchema("default");
        subTopicConfig.setPattern("TestSubTopic");
        
        // Create the instance
        InstanceConfig instance = this.createInstance(autodiscoConf, pub1, sub1, pubTopicConfig, subTopicConfig);

        GlobalConfiguration config = new GlobalConfiguration();
        config.getInstanceConfig().add(instance);
        config.setVersion("1.0.0");

        this.checkValues(config);
    }

    /**
     * @param autodiscoConf
     * @param pub1
     * @param sub1
     * @param pubTopicConfig
     * @param subTopicConfig
     * @return
     */
    private InstanceConfig createInstance(AutoDiscoveryConfig autodiscoConf, PubSocketSchema pub1,
            SubSocketSchema sub1, PubTopicConfig pubTopicConfig, SubTopicConfig subTopicConfig)
    {
        InstanceConfig instance = new InstanceConfig();
        instance.setName("instanceName");
        instance.setAutoDiscovery(autodiscoConf);
        instance.getPubSocketSchema().add(pub1);
        instance.getSubSocketSchema().add(sub1);
        instance.getPubTopic().add(pubTopicConfig);
        instance.getSubTopic().add(subTopicConfig);
        
        return instance;
    }

    /** @return PubSocketSchema created */
    private PubSocketSchema createPublisher()
    {
        PubSocketSchema pub1 = new PubSocketSchema();
        pub1.setPubRateLimit(10L);
        pub1.setName("pub1");
        pub1.setTransportMedia("tcp");
        pub1.setTransportInterface("transpInterface");
        pub1.setMinPort(40000);
        pub1.setMaxPort(40100);
        pub1.setMaxNumPorts(2);
       
        return pub1;
    }

    /** @return AutoDiscoveryConfig created */
    private AutoDiscoveryConfig createAutoDiscoConfig()
    {
        AutoDiscoveryConfig autodiscoConf = new AutoDiscoveryConfig();
        autodiscoConf.setAddresses("224.2.2.3");
        autodiscoConf.setNodeType(NodeType.CLIENT);
        autodiscoConf.setReconnectionInterval(5);
        autodiscoConf.setReconnectionTries(10);
        autodiscoConf.setRefreshInterval(5000L);
        autodiscoConf.setTimeToLive(1200);
        autodiscoConf.setTransportMedia(TransportMediaType.MULTICAST);
        
        return autodiscoConf;
    }

    private void checkValues(final GlobalConfiguration config)
    {
        // Now check the values
        InstanceConfig instance = config.getInstanceConfig().get(0);
        Assert.assertNotNull(instance);
        Assert.assertEquals("instanceName", instance.getName());

        // Check the autodiscovery information
        this.checkAutodiscoInfo(instance);
        
        // Check the publishers information
        final List<PubSocketSchema> publishers = instance.getPubSocketSchema();
        Assert.assertEquals(1, publishers.size());

        // Check first publisher
        this.checkFirstPublisher(publishers);
      
        // Check the subscribers information
        final List<SubSocketSchema> subscribers = instance.getSubSocketSchema();
        Assert.assertEquals(1, subscribers.size());

        // Check first subscriber
        final SubSocketSchema subscriber = subscribers.get(0);
        Assert.assertTrue(subscriber.getName() == "sub1");
        Assert.assertTrue(subscriber.getSubRateLimit() == 10L);

        // Check publisher topic 
        Assert.assertEquals(instance.getPubTopic().get(0).getSocketSchema(),"default");
        Assert.assertEquals(instance.getPubTopic().get(0).getPattern(),"TestPubTopic");

        // Check subscriber topic 
        Assert.assertEquals(instance.getSubTopic().get(0).getSocketSchema(),"default");
        Assert.assertEquals(instance.getSubTopic().get(0).getPattern(),"TestSubTopic");
        
        Assert.assertEquals("1.0.0", config.getVersion());
    }

    /**
     * @param publishers
     */
    private void checkFirstPublisher(final List<PubSocketSchema> publishers)
    {
        final PubSocketSchema publisher = publishers.get(0);
        
        Assert.assertEquals("pub1", publisher.getName());
        Assert.assertEquals("transpInterface", publisher.getTransportInterface());
        Assert.assertEquals("tcp", publisher.getTransportMedia());
        Assert.assertEquals(10L, publisher.getPubRateLimit().longValue());
        Assert.assertEquals(40000, publisher.getMinPort());
        Assert.assertEquals(40100, publisher.getMaxPort().intValue());
        Assert.assertEquals(2, publisher.getMaxNumPorts().intValue());
    }

    /**
     * @param instance
     */
    private void checkAutodiscoInfo(InstanceConfig instance)
    {
        final AutoDiscoveryConfig autodiscoConf = instance.getAutoDiscovery();
      
        Assert.assertEquals("224.2.2.3", autodiscoConf.getAddresses());
        Assert.assertEquals(NodeType.CLIENT, autodiscoConf.getNodeType());
        Assert.assertEquals(5, autodiscoConf.getReconnectionInterval().intValue());
        Assert.assertEquals(10, autodiscoConf.getReconnectionTries().intValue());
        Assert.assertEquals(5000L, autodiscoConf.getRefreshInterval().longValue());
        Assert.assertEquals(1200, autodiscoConf.getTimeToLive().intValue());        
        Assert.assertEquals(TransportMediaType.MULTICAST, autodiscoConf.getTransportMedia());
    }
}