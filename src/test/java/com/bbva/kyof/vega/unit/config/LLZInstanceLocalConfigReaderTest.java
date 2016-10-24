package com.bbva.kyof.vega.unit.config;

import static junit.framework.Assert.fail;

import java.util.List;

import junit.framework.Assert;

import com.bbva.kyof.vega.config.LLZInstanceLocalConfigReader;
import com.bbva.kyof.vega.config.general.InstanceConfig;
import com.bbva.kyof.vega.config.general.PubSocketSchema;
import com.bbva.kyof.vega.config.general.PubTopicConfig;
import com.bbva.kyof.vega.config.general.RespSocketSchema;
import com.bbva.kyof.vega.config.general.SubSocketSchema;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.protocol.LLZInstanceContext;
import com.bbva.kyof.vega.protocol.LLZManagerParams;

/**
 * Testing methods for the {@link LLZInstanceLocalConfigReader}
 * Created by XE48745 on 15/09/2015.
 */
public class LLZInstanceLocalConfigReaderTest
{
   
    /** File containing the configuration */
    private static final String validConfigFile = LLZInstanceLocalConfigReaderTest.class.getClassLoader().getResource("config/validConfiguration.xml").getPath();

    /** Malformed configuration file */
    private static final String malformedConfigFile = LLZInstanceLocalConfigReaderTest.class.getClassLoader().getResource("config/malformedConfiguration.xml").getPath();

    
   
    @org.junit.Test
    public void loadAndValidateConfig()
    {
         try
        {
            LLZManagerParams params = new LLZManagerParams.Builder("SampleConfig", validConfigFile).build();
            LLZInstanceContext instanceContext =  new LLZInstanceContext(params); 
            // Load and validate the global instance configuration
            final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(params.getInstanceName(), params.getConfigurationFile());
            instanceConfigReader.loadAndValidateConfig();

            // Load the configuration into the context
            instanceContext.setInstanceConfiguration(instanceConfigReader.getLoadedConfig());
                 
            final InstanceConfig config = instanceContext.getInstanceConfig();
            
            Assert.assertNotNull(config);

            // Check the publishers information
            final List<PubSocketSchema> publishers = config.getPubSocketSchema();
            Assert.assertEquals(publishers.size(), 1);

            // Check first Publisher schema
            this.checkFirstPublisherSchema(publishers);

            final List<PubTopicConfig> pubTopics = config.getPubTopic();
            Assert.assertEquals(".*", pubTopics.get(0).getPattern());
            Assert.assertEquals("default", pubTopics.get(0).getSocketSchema());

            // Check the subscribers information
            final List<SubSocketSchema> subscribers = config.getSubSocketSchema();
                        
            // Check first Subscriber schema
            final SubSocketSchema subscriber = subscribers.get(0);           
            Assert.assertEquals("default", subscriber.getName());
            Assert.assertEquals(100, subscriber.getSubRateLimit().longValue());
            
            // Check the responders information
            final List<RespSocketSchema> responders = config.getRespSocketSchema();
            Assert.assertEquals(responders.size(), 1);

            // Check first Publisher schema
            this.checkFirstResponderSchema(responders);
            
            //TODO check also autodiscoConfig and requester.
        }
        catch (LLZException e)
        {
            fail("Error creating the LLZInstanceContext Instance. Error ["+e.getMessage()+"]");
        }   
    
    }

    /**
     * @param publishers
     */
    private void checkFirstPublisherSchema(final List<PubSocketSchema> publishers)
    {
        final PubSocketSchema publisher = publishers.get(0);
        
        Assert.assertEquals("default", publisher.getName());
        Assert.assertEquals("*", publisher.getTransportInterface());
        Assert.assertEquals("tcp", publisher.getTransportMedia());
        Assert.assertEquals(40000, publisher.getMinPort());
        Assert.assertEquals(40100, publisher.getMaxPort().intValue());
        Assert.assertEquals(2, publisher.getMaxNumPorts().intValue());
        Assert.assertEquals(100, publisher.getPubRateLimit().longValue());
    }

    /**
     * @param publishers
     */
    private void checkFirstResponderSchema(final List<RespSocketSchema> responders)
    {
        final RespSocketSchema responder = responders.get(0);
        
        Assert.assertEquals("default", responder.getName());
        Assert.assertEquals("*", responder.getTransportInterface());
        Assert.assertEquals("tcp", responder.getTransportMedia());
        Assert.assertEquals(52000, responder.getMinPort());
        Assert.assertEquals(53000, responder.getMaxPort().intValue());
        Assert.assertEquals(45, responder.getMaxNumPorts().intValue());
    }
    
    @org.junit.Test(expected = LLZException.class)
    public void nonExistingConfigLoad() throws java.lang.Exception
    {
        LLZManagerParams params = new LLZManagerParams.Builder("nonExistingConfig", "BadFileName").build();

        // Load and validate the global instance configuration
        final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(params.getInstanceName(), params.getConfigurationFile());
        instanceConfigReader.loadAndValidateConfig();
    }

    @org.junit.Test(expected = LLZException.class)
    public void wrongInstanceNameConfigValidation() throws java.lang.Exception
    {
        LLZManagerParams params = new LLZManagerParams.Builder("wrongInstanceName", validConfigFile).build();

        // Load and validate the global instance configuration
        final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(params.getInstanceName(), params.getConfigurationFile());
        instanceConfigReader.loadAndValidateConfig();
    }

    @org.junit.Test(expected = LLZException.class)
    public void wrongFormatConfigLoad() throws java.lang.Exception
    {      
        LLZManagerParams params = new LLZManagerParams.Builder("BadSampleConfig", malformedConfigFile).build();

        // Load and validate the global instance configuration
        final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(params.getInstanceName(), params.getConfigurationFile());
        instanceConfigReader.loadAndValidateConfig();
        
    }
}
