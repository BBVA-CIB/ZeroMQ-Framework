package com.bbva.kyof.vega.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.config.general.AutoDiscoveryConfig;
import com.bbva.kyof.vega.config.general.InstanceConfig;
import com.bbva.kyof.vega.config.general.NodeType;
import com.bbva.kyof.vega.config.general.PubSocketSchema;
import com.bbva.kyof.vega.config.general.PubTopicConfig;
import com.bbva.kyof.vega.config.general.ReqSocketSchema;
import com.bbva.kyof.vega.config.general.ReqTopicConfig;
import com.bbva.kyof.vega.config.general.RespSocketSchema;
import com.bbva.kyof.vega.config.general.RespTopicConfig;
import com.bbva.kyof.vega.config.general.SubSocketSchema;
import com.bbva.kyof.vega.config.general.SubTopicConfig;
import com.bbva.kyof.vega.config.general.TransportMediaType;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.util.PatternEquals;


/**
 * Wrapper around XJC generated InstanceConfig
 * Generated with:
 * xjc -p com.bbva.kyof.zmq.config.general D:\eclipseworkspacejava\ZeroMQ\FrameworkZeroMq\src\main\resources\xsd\zeromqConfig.xsd
 */
public class LLZInstanceConfigWrapper extends InstanceConfig
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZInstanceConfigWrapper.class);
    
    /** The auto-discovery configuration */
    private AutoDiscoveryConfig autodiscoConfig = new AutoDiscoveryConfig();

    /** Stores all the Publisher Socket schemas */
    private final HashMap<String, PubSocketSchema> pubSocketSchemas = new HashMap<>();

    /** Stores all the Subscriber Socket schemas */
    private final HashMap<String, SubSocketSchema> subSocketSchemas = new HashMap<>();
   
    /** Stores all the Requester Socket schemas */
    private final HashMap<String, ReqSocketSchema> reqSocketSchemas = new HashMap<>();
    
    /** Stores all the Responder Socket schemas */
    private final HashMap<String, RespSocketSchema> respSocketSchemas = new HashMap<>();

    /** Stores all the publisher topics */
    private final LinkedHashMap<PatternEquals, PubTopicConfig> pubTopicConfigs = new LinkedHashMap<>();
    
    /** Stores all the subscriber topics */
    private final LinkedHashMap<PatternEquals, SubTopicConfig> subTopicConfigs = new LinkedHashMap<>();
     
    /** Stores all the requester topics */
    private final LinkedHashMap<PatternEquals, ReqTopicConfig> reqTopicConfigs = new LinkedHashMap<>();
    
    /** Stores all the responder topics */
    private final LinkedHashMap<PatternEquals, RespTopicConfig> respTopicConfigs = new LinkedHashMap<>();

    
    /**
     * Builds a copy-wrapper around InstanceConfig which contains the same information
     *
     * @param config Object to configures instances programmatically
     */
    public LLZInstanceConfigWrapper(final InstanceConfig config)
    {
        super.setName(config.getName());
        super.setUseNativeZeromqFiltering(config.isUseNativeZeromqFiltering());
        
        if (config.getAutoDiscovery() == null)
        {
            // DEFAULT VALUES FOR AUTO-DISCOVERY CONFIGURATION
            AutoDiscoveryConfig autodiscoCfg = new AutoDiscoveryConfig();
            
            autodiscoCfg.setTimeToLive(1200);
            autodiscoCfg.setRefreshInterval(5000L);
            autodiscoCfg.setNodeType(NodeType.CLIENT);
            autodiscoCfg.setTransportMedia(TransportMediaType.MULTICAST);
            autodiscoCfg.setAddresses("224.2.2.3");
            autodiscoCfg.setReconnectionInterval(2); 
            autodiscoCfg.setReconnectionTries(10);;      

            this.setAutoDiscoConfig(autodiscoCfg);
        } 
        else 
        {
            this.setAutoDiscoConfig(config.getAutoDiscovery());
         }
        
        List<PubSocketSchema> pubSockets = super.getPubSocketSchema();
        pubSockets.addAll(config.getPubSocketSchema());
        
        List<SubSocketSchema> subSockets = super.getSubSocketSchema();
        subSockets.addAll(config.getSubSocketSchema());
        
        List<ReqSocketSchema> reqSockets = super.getReqSocketSchema();
        reqSockets.addAll(config.getReqSocketSchema());
        
        List<RespSocketSchema> respSockets = super.getRespSocketSchema();
        respSockets.addAll(config.getRespSocketSchema());
        
        List<PubTopicConfig> pubTopics = super.getPubTopic();
        pubTopics.addAll(config.getPubTopic());
        
        List<SubTopicConfig> subTopics = super.getSubTopic();
        subTopics.addAll(config.getSubTopic());
        
        List<ReqTopicConfig> reqTopics = super.getReqTopic();
        reqTopics.addAll(config.getReqTopic());
        
        List<RespTopicConfig> respTopics = super.getRespTopic();
        respTopics.addAll(config.getRespTopic());
    }

    
    /** @return The auto-discovery configuration */
    public AutoDiscoveryConfig getAutodiscoConfig()
    {
        return this.autodiscoConfig;
    }
    
    
    /** Set the auto-discovery configuration */
    public void setAutoDiscoConfig (final AutoDiscoveryConfig autodiscoConfig)
    {
        this.autodiscoConfig = autodiscoConfig;
    }
    
    
    /**
     * Adds Publisher Socket Schema
     *
     * @param pubSocketSchema PubSocketSchema to be added
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addPubSchema(final PubSocketSchema pubSocketSchema) throws LLZException
    {
        if (!this.pubSocketSchemas.containsKey(pubSocketSchema.getName()))
        {
            this.pubSocketSchemas.put(pubSocketSchema.getName(), pubSocketSchema);
        }
        else
        {
            LOGGER.error("Duplicated Publisher socket schema name [{}] found in the xml configuration file.",
                    pubSocketSchema.getName());
            throw new LLZException("Duplicated Publisher socket schema name found in the xml configuration file:"
                    + pubSocketSchema.getName());
        }
    }

    
    /**
     * Adds Subscriber Socket Schema
     *
     * @param subSocketSchema SubSocketSchema to be added
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addSubSchema(final SubSocketSchema subSocketSchema) throws LLZException
    {
        if (!this.subSocketSchemas.containsKey(subSocketSchema.getName()))
        {
            this.subSocketSchemas.put(subSocketSchema.getName(), subSocketSchema);
        }
        else
        {
            LOGGER.error("Duplicated Subscriber socket schema name [{}] found in the xml configuration file.",
                    subSocketSchema.getName());
            throw new LLZException("Duplicated Subscriber socket schema name found in the xml configuration file:"
                    + subSocketSchema.getName());
        }
    }
    
    
    /**
     * Adds Requester Socket Schema
     *
     * @param reqSocketSchema ReqSocketSchema to be added
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addReqSchema(final ReqSocketSchema reqSocketSchema) throws LLZException
    {
        if (!this.reqSocketSchemas.containsKey(reqSocketSchema.getName()))
        {
            this.reqSocketSchemas.put(reqSocketSchema.getName(), reqSocketSchema);
        }
        else
        {
            LOGGER.error("Duplicated Requester socket schema name [{}] found in the xml configuration file.",
                    reqSocketSchema.getName());
            throw new LLZException("Duplicated Requester socket schema name found in the xml configuration file:"
                    + reqSocketSchema.getName());
        }
    }

    
    /**
     * Adds Responder Socket Schema
     *
     * @param respSocketSchema RespSocketSchema to be added
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addRespSchema(final RespSocketSchema respSocketSchema) throws LLZException
    {
        if (!this.respSocketSchemas.containsKey(respSocketSchema.getName()))
        {
            this.respSocketSchemas.put(respSocketSchema.getName(), respSocketSchema);
        }
        else
        {
            LOGGER.error("Duplicated Responder socket schema name [{}] found in the xml configuration file.",
                    respSocketSchema.getName());
            throw new LLZException("Duplicated Responder socket schema name found in the xml configuration file:"
                    + respSocketSchema.getName());
        }
    }
    
    /**
     * Returns Publisher Socket schema
     *
     * @param name Name of the PubSocketSchema (PubSocketSchema.getName() )
     * @return PubSocketSchema or null if it does not exist
     */
    public PubSocketSchema getPubSocketSchema(final String name)
    {
        return this.pubSocketSchemas.get(name);
    }

    
    /**
     * Returns Subscriber Socket schema
     *
     * @param name Name of the SubSocketSchema (SubSocketSchema.getName() )
     * @return SubSocketSchema or null if it does not exist
     */
    public SubSocketSchema getSubSocketSchema(final String name)
    {
        return this.subSocketSchemas.get(name);
    }
    
    
    /**
     * Returns Requester Socket schema
     *
     * @param name Name of the ReqSocketSchema (ReqSocketSchema.getName() )
     * @return ReqSocketSchema or null if it does not exist
     */
    public ReqSocketSchema getReqSocketSchema(final String name)
    {
        return this.reqSocketSchemas.get(name);
    }
    
    
    /**
     * Returns Responder Socket schema
     *
     * @param name Name of the RespSocketSchema (RespSocketSchema.getName() )
     * @return RespSocketSchema or null if it does not exist
     */
    public RespSocketSchema getRespSocketSchema(final String name)
    {
        return this.respSocketSchemas.get(name);
    }
    
    
    /**
     * Adds a Pub topic schema
     *
     * @param pubTopicCfg configuration
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addPubTopicCfg(final PubTopicConfig pubTopicCfg) throws LLZException
    {
        final String pattern = pubTopicCfg.getPattern();

        PatternEquals patternEquals = new PatternEquals(Pattern.compile(pattern));
        if (this.pubTopicConfigs.containsKey(patternEquals))
        {
            LOGGER.error("Duplicated pub topic name pattern [{}] found in the xml configuration file.", pattern);
            throw new LLZException("Duplicated pub topic name pattern found in the xml configuration file:" + pattern);
        }
        else
        {
            this.pubTopicConfigs.put(patternEquals, pubTopicCfg);
        }
        
    }

    
    /**
     * Adds a Sub topic schema
     *
     * @param subTopicCfg configuration
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addSubTopicCfg(final SubTopicConfig subTopicCfg) throws LLZException
    {
        final String pattern = subTopicCfg.getPattern();
        
        PatternEquals patternEquals = new PatternEquals(Pattern.compile(pattern));
        if (this.subTopicConfigs.containsKey(patternEquals))
        {
            LOGGER.error("Duplicated sub topic name pattern [{}] found in the xml configuration file.", pattern);
            throw new LLZException("Duplicated sub topic name pattern found in the xml configuration file:" + pattern);
        }
        else
        {
            this.subTopicConfigs.put(patternEquals, subTopicCfg);
        }
        
    }
    
    
    /**
     * Adds a Req topic schema
     *
     * @param reqTopicCfg configuration
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addReqTopicCfg(final ReqTopicConfig reqTopicCfg) throws LLZException
    {
        final String pattern = reqTopicCfg.getPattern();
        
        PatternEquals patternEquals = new PatternEquals(Pattern.compile(pattern));
        if (this.reqTopicConfigs.containsKey(patternEquals))
        {
            LOGGER.error("Duplicated req topic name pattern [{}] found in the xml configuration file.", pattern);
            throw new LLZException("Duplicated req topic name pattern found in the xml configuration file:" + pattern);
        }
        else
        {
            this.reqTopicConfigs.put(patternEquals, reqTopicCfg);
        }
    }

    
    /**
     * Adds a Resp topic schema
     *
     * @param respTopicCfg configuration
     * @throws LLZException exception thrown if there a duplicate
     */
    public void addRespTopicCfg(final RespTopicConfig respTopicCfg) throws LLZException
    {
        final String pattern = respTopicCfg.getPattern();

        PatternEquals patternEquals = new PatternEquals(Pattern.compile(pattern));
        if (this.respTopicConfigs.containsKey(patternEquals))
        {
            LOGGER.error("Duplicated resp topic name pattern [{}] found in the xml configuration file.", pattern);
            throw new LLZException("Duplicated resp topic name pattern found in the xml configuration file:" + pattern);
        }
        else
        {
            this.respTopicConfigs.put(patternEquals, respTopicCfg);
        }
    }

    /**
     * Returns Config pubTopicCfg, it will look in all stored patterns to first one in the list that matches.
     *
     * This means configurations should contain the most specific patterns first.
     *
     * @param name Name of the pubTopicCfg (it will be matched again all patterns )
     * @return Config pubTopicCfg or null if it does not exist
     */
    public PubTopicConfig getPubTopicCfg(final String name) throws LLZException
    {
        for (Map.Entry<PatternEquals, PubTopicConfig> pubTopicCfg : this.pubTopicConfigs.entrySet())
        {
            if (pubTopicCfg.getKey().matches(name))
            {
                return pubTopicCfg.getValue();
            }
        }

        LOGGER.error("Tried to create topic/publisher with name [{}] which has no configuration. ", name);

        throw new LLZException("Error, no configuration exists for Pub topic " + name);
    }
    
    /**
     * Returns Config subTopicCfg, it will look in all stored patterns to first one in the list that matches.
     *
     * This means configurations should contain the most specific patterns first.
     *
     * @param name Name of the subTopicCfg (it will be matched again all patterns )
     * @return Config subTopicCfg or null if it does not exist
     */
    public SubTopicConfig getSubTopicCfg(final String name)
    {
        for (Map.Entry<PatternEquals, SubTopicConfig> subTopicCfg : this.subTopicConfigs.entrySet())
        {
            if (subTopicCfg.getKey().matches(name))
            {
                return subTopicCfg.getValue();
            }
        }

        return null;
    }

    /**
     * Return true if the subscriber topic is configured in any of the config file patterns
     *
     * @param name name of the topic
     * @return true if configured
     */
    public boolean isSubTopicConfigured(final String name)
    {
        for (Map.Entry<PatternEquals, SubTopicConfig> subTopicCfg : this.subTopicConfigs.entrySet())
        {
            if (subTopicCfg.getKey().matches(name))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns Config reqTopicCfg, it will look in all stored patterns to first one in the list that matches.
     *
     * This means configurations should contain the most specific patterns first.
     *
     * @param name Name of the reqTopicCfg (it will be matched again all patterns )
     * @return Config reqTopicCfg or null if it does not exist
     */
    public ReqTopicConfig getReqTopicCfg(final String name) throws LLZException
    {
        for (Map.Entry<PatternEquals, ReqTopicConfig> reqTopicCfg : this.reqTopicConfigs.entrySet())
        {
            if (reqTopicCfg.getKey().matches(name))
            {
                return reqTopicCfg.getValue();
            }
        }
        
        LOGGER.error("Tried to create topic/requester with name [{}] which has no configuration. ", name);
        
        throw new LLZException("Error, no configuration exists for Req topic " + name);
    }

    /**
     * Return true if the requester topic is configured in any of the config file patterns
     *
     * @param name name of the topic
     * @return true if configured
     */
    public boolean isReqTopicConfigured(final String name)
    {
        for (Map.Entry<PatternEquals, ReqTopicConfig> reqTopicCfg : this.reqTopicConfigs.entrySet())
        {
            if (reqTopicCfg.getKey().matches(name))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns Config respTopicCfg, it will look in all stored patterns to first one in the list that matches.
     *
     * This means configurations should contain the most specific patterns first.
     *
     * @param name Name of the respTopicCfg (it will be matched again all patterns )
     * @return Config respTopicCfg or null if it does not exist
     */
    public RespTopicConfig getRespTopicCfg(final String name) throws LLZException
    {
        for (Map.Entry<PatternEquals, RespTopicConfig> respTopicCfg : this.respTopicConfigs.entrySet())
        {
            if (respTopicCfg.getKey().matches(name))
            {
                return respTopicCfg.getValue();
            }
        }

        LOGGER.error("Tried to create topic/responder with name [{}] which has no configuration. ", name);

        throw new LLZException("Error, no configuration exists for Resp topic " + name);
    }

}
