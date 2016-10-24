package com.bbva.kyof.vega.config;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.bbva.kyof.vega.config.general.AutoDiscoveryConfig;
import com.bbva.kyof.vega.config.general.GlobalConfiguration;
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

/**
 * This class helps to read the instance configuration and perform configuration validations
 */
public final class LLZInstanceLocalConfigReader
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZInstanceLocalConfigReader.class);

    /** Location of the XSD describing the XML configuration file */
    public static final String XSD_CONFIG_FILE = "/xsd/zeromqConfig.xsd";

    /** Url with the package that contains the configuration general classes */
    public static final String URL_BASE = "com.bbva.kyof.zmq.config.general";
    
    /** Instance name */
    private final String instanceName;

    /** Path to the configuration file */
    private final String configFile;

    /** Stores the loaded configuration */
    private LLZInstanceConfigWrapper loadedConfig = null;

    /**
     * Create a new instance of the configuration reader
     *
     * @param instanceName name of the instance
     * @param configFile configuration file to load
     */
    public LLZInstanceLocalConfigReader(final String instanceName, final String configFile)
    {
        this.instanceName = instanceName;
        this.configFile = configFile;
    }

    /**
     * This function load and validate the instance inside the instance configuration file
     *
     * @throws LLZException exception thrown if the configuration cannot be loaded or is invalid
     */
    public void loadAndValidateConfig() throws LLZException
    {
        LOGGER.info("Loading the xml configuration file [{}] for instance [{}]", this.configFile, this.instanceName);

        // First unmarshall the configuration
        final GlobalConfiguration configuration = unmarshallConfiguration(configFile);

        // Now find the instance configuration
        this.loadedConfig = new LLZInstanceConfigWrapper(this.findInstance(configuration, this.instanceName));

        // Now clean the configuration. It will set proper values to null optional entries when not settled
        this.validateAndCleanConfiguration(this.loadedConfig);
    }

    /** Get the loaded configuration */
    public LLZInstanceConfigWrapper getLoadedConfig()
    {
        return this.loadedConfig;
    }

    
    /**
     * Unmarshall the configuration using JAXB
     *
     * @param configFile path to the file
     * @return the unmarshalled configuration
     *
     * @throws LLZException exception thrown if there is any problem
     */
    @SuppressWarnings("unchecked")
    private GlobalConfiguration unmarshallConfiguration(final String configFile) throws LLZException
    {
        try
        {
            final File file = new File(configFile);
            final JAXBContext jaxbContext = JAXBContext.newInstance(URL_BASE);

            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            final SchemaFactory factory = SchemaFactory.newInstance(LLZConfigReaderConstants.W3_SCHEMA);

            final Schema schema = factory.newSchema(new StreamSource(LLZInstanceLocalConfigReader.class.getResourceAsStream(XSD_CONFIG_FILE)));

            jaxbUnmarshaller.setSchema(schema);
            return ((JAXBElement<GlobalConfiguration>) jaxbUnmarshaller.unmarshal(file)).getValue();
        }
        catch (final SAXException | JAXBException e)
        {
            LOGGER.error("Error parsing the xml file [{}]", configFile, e);

            throw new LLZException("Error loading the xml configuration file passed.", e);
        }
    }

    /**
     * Find the given instance configuration inside the global configuration read
     *
     * @param instanceName  Name of the instance
     * @return the instance configuration
     */
    private InstanceConfig findInstance(final GlobalConfiguration configuration, final String instanceName) throws LLZException
    {
        for(final InstanceConfig instance : configuration.getInstanceConfig())
        {
            if (instance.getName().equals(instanceName))
            {
                return instance;
            }
        }

        LOGGER.error("Cannot find any application in the configuration with the name [{}]", instanceName);
        throw new LLZException("Not application found with name " + instanceName);
    }

    /**
     * Checks all different configurations and clean them by setting default values for null values.
     *
     * @param loadedConfig the loaded configuration
     * @throws LLZException exception thrown if the configuration is not valid
     */
    private void validateAndCleanConfiguration(final LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {
        if(anyTopicConfigurated())
        {
            LOGGER.error("Any topic configuration found for Instance [{}] in the xml configuration file.", loadedConfig.getName());
            throw new LLZException(String.format("Any topic configuration found for Instance %s in the xml configuration file ",  loadedConfig.getName()));
        }
        
        validateAndCleanAutodiscoConfig(loadedConfig);
        addSocketSchemas(loadedConfig);
        validateTopicsConfig(loadedConfig);
    }

    /** @return True if any topic configuration is found in the xml configuration file*/
    private boolean anyTopicConfigurated()
    {
        return (loadedConfig.getPubTopic().isEmpty() &&
                loadedConfig.getSubTopic().isEmpty() &&
                loadedConfig.getReqTopic().isEmpty() &&
                loadedConfig.getRespTopic().isEmpty() );
    }

    /**
     * Clean the given auto-discovery configuration by setting default values for null values.
     * 
     * @param loadedConfig the loaded configuration
     */
    private void validateAndCleanAutodiscoConfig(LLZInstanceConfigWrapper loadedConfig)
    {
        final AutoDiscoveryConfig autodiscoCfg = loadedConfig.getAutodiscoConfig();
                
        //Initialize defaults (even if auto-discovery has a default, it only works
        //when the element is present but it has no value
        if (autodiscoCfg.getTimeToLive() == null) 
        {
            autodiscoCfg.setTimeToLive(1200);
        }
      
        if (autodiscoCfg.getRefreshInterval() == null) 
        {
            autodiscoCfg.setRefreshInterval(5000L);
        }

        if (autodiscoCfg.getNodeType() == null) 
        {
            autodiscoCfg.setNodeType(NodeType.CLIENT);
        }

        if (autodiscoCfg.getTransportMedia() == null) 
        {
            autodiscoCfg.setTransportMedia(TransportMediaType.MULTICAST);
        }

        if (autodiscoCfg.getAddresses() == null) 
        {
            autodiscoCfg.setAddresses("224.2.2.3");
        }
      
        if (autodiscoCfg.getReconnectionInterval() == null) 
        {
            autodiscoCfg.setReconnectionInterval(2);
        }
        
        if (autodiscoCfg.getReconnectionTries() == null) 
        {
            autodiscoCfg.setReconnectionTries(10);;
        }
        
        loadedConfig.setAutoDiscovery(autodiscoCfg);  
    }
        
    /**
     * Checks all topic different configurations.
     * 
     * @param loadedConfig the loaded configuration
     * @throws LLZException exception thrown if the configuration is not valid
     */
    private void validateTopicsConfig(LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {
        // Pub Topic
        for (final PubTopicConfig pubTopicCfg : loadedConfig.getPubTopic())
        {
            this.validatePubTopicCfg(pubTopicCfg, loadedConfig);
        }

        // Sub Topic
        for (final SubTopicConfig subTopicCfg : loadedConfig.getSubTopic())
        {
            this.validateSubTopicCfg(subTopicCfg, loadedConfig);
        }

        // Req Topic
        for (final ReqTopicConfig reqTopicCfg : loadedConfig.getReqTopic())
        {
            this.validateReqTopicCfg(reqTopicCfg, loadedConfig);
        }

        // Resp Topic
        for (final RespTopicConfig respTopicCfg : loadedConfig.getRespTopic())
        {
            this.validateRespTopicCfg(respTopicCfg, loadedConfig);
        }
    }

    /**
     *  Adds sockect schemas different configurations.
     *  
     * @param loadedConfig the loaded configuration 
     * @throws LLZException exception thrown if the configuration is not valid
     */
    private void addSocketSchemas(final LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {
        // Pub Socket schemas
        for (final PubSocketSchema pubSocketSchema : loadedConfig.getPubSocketSchema())
        {
            loadedConfig.addPubSchema(pubSocketSchema);
        }

        // Sub Socket schemas
        for (final SubSocketSchema subSocketSchema : loadedConfig.getSubSocketSchema())
        {
            loadedConfig.addSubSchema(subSocketSchema);
        }

        // Req Socket schemas
        for (final ReqSocketSchema reqSocketSchema : loadedConfig.getReqSocketSchema())
        {
            loadedConfig.addReqSchema(reqSocketSchema);
        }

        // Resp Socket schemas
        for (final RespSocketSchema respSocketSchema : loadedConfig.getRespSocketSchema())
        {
            loadedConfig.addRespSchema(respSocketSchema);        
        }
    }

    /**
     * Validate the publisher configuration 
     *
     * @param pubTopicCfg the publisher topic configuration
     * @param loadedConfig loadedConfig configuration wrapper to fill after validation is correct
     *
     * @throws LLZException exception thrown if the configuration is not valid
     */
    private void validatePubTopicCfg(final PubTopicConfig pubTopicCfg, final LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {        
        if ( loadedConfig.getPubSocketSchema(pubTopicCfg.getSocketSchema()) == null ) 
        {
            LOGGER.error("Schema name for Pub topic [{}] was not found in the xml configuration file.", pubTopicCfg.getPattern());
            throw new LLZException("Schema name for Pub topic was not found in the xml configuration file. Topic: " + pubTopicCfg.getPattern());
        }

        if ( pubTopicCfg.getPattern() == null )
        {
            LOGGER.error("Empty Pub topic [{}] found in the xml configuration file.", pubTopicCfg.getPattern());
            throw new LLZException("Empty Pub topic found in the xml configuration file: " + pubTopicCfg.getPattern());
        }

        loadedConfig.addPubTopicCfg(pubTopicCfg); 
    }
    
    /**
     * Validate the subscriber configuration 
     *
     * @param subTopicCfg the subscriber topic configuration
     * @param loadedConfig loadedConfig configuration wrapper to fill after validation is correct
     *
     * @throws LLZException exception thrown if the configuration is not valid
     */
    private void validateSubTopicCfg(final SubTopicConfig subTopicCfg, final LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {        
        if ( loadedConfig.getSubSocketSchema(subTopicCfg.getSocketSchema()) == null ) 
        {
            LOGGER.error("Schema name for Sub topic [{}] was not found in the xml configuration file.", subTopicCfg.getPattern());
            throw new LLZException("Schema name for Sub topic  was not found in the xml configuration file. Topic: " + subTopicCfg.getPattern());
        }

        if ( subTopicCfg.getPattern() == null )
        {
            LOGGER.error("Empty Sub topic [{}] found in the xml configuration file.", subTopicCfg.getPattern());
            throw new LLZException("Empty Sub topic found in the xml configuration file: " + subTopicCfg.getPattern());
        }

        loadedConfig.addSubTopicCfg(subTopicCfg); 
    }

    /**
     * Validate the requester configuration 
     *
     * @param reqTopicCfg the requester topic configuration
     * @param loadedConfig loadedConfig configuration wrapper to fill after validation is correct
     *
     * @throws LLZException exception thrown if the configuration is not valid
     */
    private void validateReqTopicCfg(final ReqTopicConfig reqTopicCfg, final LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {
        if ( loadedConfig.getReqSocketSchema(reqTopicCfg.getSocketSchema()) == null ) 
        {     
            LOGGER.error("Schema name for Req topic [{}] was not found in the xml configuration file.", reqTopicCfg.getPattern());
            throw new LLZException("Schema name for Req topic was not found in the xml configuration file. Topic: " + reqTopicCfg.getPattern());
        }

        if ( reqTopicCfg.getPattern() == null )
        {
            LOGGER.error("Empty Req topic [{}] found in the xml configuration file.", reqTopicCfg.getPattern());
            throw new LLZException("Empty Req topic found in the xml configuration file: " + reqTopicCfg.getPattern());
        }
        loadedConfig.addReqTopicCfg(reqTopicCfg);
    }
    
    
    /**
     * Validate the responder configuration 
     *
     * @param respTopicCfg the responder topic configuration
     * @param loadedConfig loadedConfig configuration wrapper to fill after validation is correct
     *
     * @throws  LLZException exception thrown if the configuration is not valid
     */
    private void validateRespTopicCfg(final RespTopicConfig respTopicCfg, final LLZInstanceConfigWrapper loadedConfig) throws LLZException
    {
        if ( loadedConfig.getRespSocketSchema(respTopicCfg.getSocketSchema()) == null ) 
        {
            LOGGER.error("Schema name for Resp topic [{}] was not found in the xml configuration file.", respTopicCfg.getPattern());
            throw new LLZException("Schema name for Resp topic was not found in the xml configuration file. Topic: " + respTopicCfg.getPattern());
        }

        if ( respTopicCfg.getPattern() == null )
        {
            LOGGER.error("Empty Resp topic [{}] found in the xml configuration file.", respTopicCfg.getPattern());
            throw new LLZException("Empty Resp topic found in the xml configuration file: " + respTopicCfg.getPattern());
        }
        loadedConfig.addRespTopicCfg(respTopicCfg);
    }
}
