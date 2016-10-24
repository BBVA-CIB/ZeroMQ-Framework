package com.bbva.kyof.vega.unit.config.general;

import com.bbva.kyof.vega.config.general.GlobalConfiguration;
import com.bbva.kyof.vega.config.general.ObjectFactory;
import org.junit.Test;

/**
 * Created by cnebrera on 26/10/15.
 */
public class ObjectFactoryTest
{
    @Test
    public void testCreateConfiguration() throws Exception
    {
        ObjectFactory factory = new ObjectFactory();
        factory.createGlobalConfiguration();
        factory.createInstanceConfig();
        factory.createPubSocketSchema();
        factory.createSubSocketSchema();
        factory.createPubTopicConfig();
        factory.createSubTopicConfig();
        factory.createZmqConfig(new GlobalConfiguration());
    }
}