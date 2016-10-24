package com.bbva.kyof.vega.autodiscovery.server.hazelcast;

import com.bbva.kyof.vega.autodiscovery.client.hazelcast.LLZHazelcastFactory;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class LLZHazelcastServer 
{

    /** Name of HZ cache used to communicate Publisher ZMQ */
    public static final String PUB_ENDPOINTS_CACHE_NAME = "PUB_ENDPOINT_ZEROMQ_AUTODISCOVERY_CACHE";

    /** Name of HZ cache used to communicate Responder ZMQ */
    public static final String RESP_ENDPOINT_CACHE_NAME = "RESP_ENDPOINT_ZEROMQ_AUTODISCOVERY_CACHE";

    
    public LLZHazelcastServer ()
    {
        // Create the hazelcast configuration
        final Config cfg = new Config();
        final SerializationConfig serCfg = new SerializationConfig();
        serCfg.addDataSerializableFactory(LLZHazelcastFactory.FACTORY_ID, new LLZHazelcastFactory());
        cfg.setSerializationConfig(serCfg);
              
        // Create the hazelcast maps with the end-points
        this.configureHazelcastMap(cfg, PUB_ENDPOINTS_CACHE_NAME);
        this.configureHazelcastMap(cfg, RESP_ENDPOINT_CACHE_NAME);
        
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);

    }
    

    /**
     * Configures a HazelCast map
     * 
     * @param cfg Object to configure Hazelcast programmatically
     * @param mapName Map Name
     */
    private void configureHazelcastMap(final Config cfg, final String mapName)
    {
        final MapConfig myMapConfig = new MapConfig();

        myMapConfig.setName(mapName);
        myMapConfig.setMaxIdleSeconds(1200); 
        // Set in-memory format in object so we save deserialization time
        myMapConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
       
        cfg.addMapConfig(myMapConfig);       
    }

    
    public static void main(String[] args) 
    {
        // Create a server
        final LLZHazelcastServer server = new LLZHazelcastServer();
    }
   

   
}