package com.bbva.kyof.vega.autodiscovery.client.hazelcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodisc;
import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPointChangeListener;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutoDiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import com.bbva.kyof.vega.config.LLZInstanceConfigWrapper;
import com.bbva.kyof.vega.exception.LLZException;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;

/**
 * Hazelcast Implementation for autodiscovery
 *
 * THIS CLASS IS NOT THREAD SAFE! THREADING SHOULD BE HANDLED EXTERNALLY
 */
public class LLZHazelcastManager implements ILLZAutodisc
{
    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZHazelcastManager.class);

    /** Name of HZ cache used to communicate Publisher ZMQ */
    public static final String PUB_ENDPOINTS_CACHE_NAME = "PUB_ENDPOINT_ZEROMQ_AUTODISCOVERY_CACHE";

    /** Name of HZ cache used to communicate Publisher ZMQ */
    public static final String RESP_ENDPOINT_CACHE_NAME = "RESP_ENDPOINT_ZEROMQ_AUTODISCOVERY_CACHE";

    /** Name of Id generator used to get hazelcast pool ids */
    public static final String ZERO_MQIDGENERATOR = "ZeroMQIDgenerator";
    
    /** The configuration of the manager instance */
    private final LLZInstanceConfigWrapper instanceConfig;

    /** Hazelcast instance */
    private  HazelcastInstance hazelcastInstance;

    /** Unique IDs generator */
    private IdGenerator idGenerator;

    /** Autodiscovery publisher for publisher end point types */
    private final LLZHazelcastEndPointPub pubEndPointsPublisher;

    /** Autodiscovery publisher for requester end point types */
    private final LLZHazelcastEndPointPub respEndPointsPublisher;
    
    /** Autodiscovery subscriber for publisher type end points */
    private final LLZHazelcastEndPointSub pubTypeEndPointsSubscriber;

    /** Autodiscovery subscriber for responder type end points */
    private final LLZHazelcastEndPointSub respTypeEndPointsSubscriber;

    /** Hazelcast map which distributes info across all ZMQ instances */
    private final IMap<Long, ILLZAutodiscTopicEndPoint> distributedPubEndPointsByTopicId;

    /** Hazelcast map which distributes info across all ZMQ instances */
    private final IMap<Long, ILLZAutodiscTopicEndPoint> distributedRespEndPointsByTopicId;

    /** Runnable task that will refresh the end-points to keep them alive in Hazelcast */
    private final EndPointsKeepAliveTask keepAliveTask;

    /**
     * Constructor which initializes Hazelcast instances
     * @param instanceConfig 
     */
    public LLZHazelcastManager(final LLZInstanceConfigWrapper instanceConfig)
    {
        
        this.instanceConfig = instanceConfig;
        
        this.setHazelcastInstance();

        // Maps of end-points distributed in hazelcast for both pub and resp endpoint types
        this.distributedPubEndPointsByTopicId = this.hazelcastInstance.getMap(PUB_ENDPOINTS_CACHE_NAME);
        this.distributedRespEndPointsByTopicId = this.hazelcastInstance.getMap(RESP_ENDPOINT_CACHE_NAME);

        // Create end point publisher managers for publishers and responder end point types
        this.pubEndPointsPublisher = new LLZHazelcastEndPointPub(this.distributedPubEndPointsByTopicId);
        this.respEndPointsPublisher = new LLZHazelcastEndPointPub(this.distributedRespEndPointsByTopicId);

        // Create end point subscriber managers for publishers and responder end point types
        this.pubTypeEndPointsSubscriber = new LLZHazelcastEndPointSub(this.distributedPubEndPointsByTopicId);
        this.respTypeEndPointsSubscriber = new LLZHazelcastEndPointSub(this.distributedRespEndPointsByTopicId);

        // Create and start the keepalive task to refresh the created end-points
        this.keepAliveTask = new EndPointsKeepAliveTask();
        final Thread keepaliveThread = new Thread(this.keepAliveTask, "EndPointsKeepAliveTask");
        keepaliveThread.start();
    }

    private void setHazelcastInstance()
    {
        switch (this.instanceConfig.getAutodiscoConfig().getNodeType())
        {
            case CLIENT:
                this.hazelcastInstance = this.initializeHazelcastClient();
                // Id generator for unique ids in auto-discovery
                this.idGenerator = this.hazelcastInstance.getIdGenerator(ZERO_MQIDGENERATOR);
                break;
            case STORAGE_DISTRIBUTED:
                this.hazelcastInstance = this.initializeHazelcast();
                // Id generator for unique ids in auto-discovery
                this.idGenerator =  this.hazelcastInstance.getIdGenerator(ZERO_MQIDGENERATOR);

                break;
            default:
                break;
        }
    }

    /**
     * 
     * Initializes Hazelcast instances
     * 
     * @return the created instance
     */
    private HazelcastInstance initializeHazelcast()
    {
        final Config cfg = this.createHazelcastConfig();
        
        // Return the created instance
        return Hazelcast.newHazelcastInstance(cfg);
    }

    /**
     * @return
     */
    private Config createHazelcastConfig()
    {
        // Create the hazelcast configuration
        final Config cfg = new Config();
        final SerializationConfig serCfg = new SerializationConfig();
        serCfg.addDataSerializableFactory(LLZHazelcastFactory.FACTORY_ID, new LLZHazelcastFactory());
        cfg.setSerializationConfig(serCfg);
      
        // Configure transport media and adresses 
        this.configureTransportMedia(cfg);
        
        // Create the hazelcast maps with the end-points
        this.configureHazelcastMap(cfg, PUB_ENDPOINTS_CACHE_NAME);
        this.configureHazelcastMap(cfg, RESP_ENDPOINT_CACHE_NAME);
        
        return cfg;
    }
    
    /**
     * 
     * Initializes Hazelcast instances
     * 
     * @return the created instance
     */
    private HazelcastInstance initializeHazelcastClient()
    {        
        // Create the client configuration
        ClientConfig clientConfig = new ClientConfig(); 
        

        final SerializationConfig serCfg = new SerializationConfig();
        serCfg.addDataSerializableFactory(LLZHazelcastFactory.FACTORY_ID, new LLZHazelcastFactory());
        clientConfig.setSerializationConfig(serCfg);

        clientConfig.getNetworkConfig().setConnectionAttemptLimit(10);
        clientConfig.getNetworkConfig().setConnectionAttemptPeriod(24 * 60);
        clientConfig.getNetworkConfig().setConnectionTimeout(this.instanceConfig.getAutodiscoConfig().getTimeToLive());
        clientConfig.getNetworkConfig().addAddress(this.instanceConfig.getAutodiscoConfig().getAddresses());
        
        // Return the created instance
        return  HazelcastClient.newHazelcastClient(clientConfig);
    }

    /**
     * Configures transport media and address/es
     * 
     * @param cfg
     */
    private void configureTransportMedia(final Config cfg)
    {
        NetworkConfig network = cfg.getNetworkConfig();
        JoinConfig joinConfig = network.getJoin();

        switch (this.instanceConfig.getAutodiscoConfig().getTransportMedia())
        {
            case MULTICAST:
                this.setMultiCastConfig(joinConfig);
                break;
            case TCP_IP:
                this.setTcpIpConfig(joinConfig);
                break;
            default:
                LOGGER.debug("Unexpected Transport Media configurated for Auto-Discovery");
                break;
        }
    }

    /**
     * 
     * @param joinConfig
     */
    private void setMultiCastConfig(final JoinConfig joinConfig)
    {
        MulticastConfig multicastCfg = new MulticastConfig();
        
        multicastCfg.setEnabled(true);
        multicastCfg.setMulticastGroup(this.instanceConfig.getAutodiscoConfig().getAddresses());
        multicastCfg.setMulticastTimeoutSeconds(this.instanceConfig.getAutodiscoConfig().getReconnectionInterval());

        joinConfig.setMulticastConfig(multicastCfg);   
        joinConfig.getTcpIpConfig().setEnabled(false);
    }
    
    /**
     * 
     * @param joinConfig
     */
    private void setTcpIpConfig(final JoinConfig joinConfig)
    {
        TcpIpConfig tcpIpCfg = new TcpIpConfig();
        
        tcpIpCfg.setEnabled(true);
        // Adds a 'well known' member. A member can be a comma separated string
        tcpIpCfg.addMember(this.instanceConfig.getAutodiscoConfig().getAddresses());
        joinConfig.setTcpIpConfig(tcpIpCfg);       
        joinConfig.getMulticastConfig().setEnabled(false);
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

        // TODO check this values, 1200 seconds is a lot of time before considering a publisher down
        myMapConfig.setMaxIdleSeconds(this.instanceConfig.getAutodiscoConfig().getTimeToLive()); 
      
// TODO        myMapConfig.setTimeToLiveSeconds(300);
        
        // Set in-memory format in object so we save deserialization time
        myMapConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
        cfg.addMapConfig(myMapConfig);       
    }

    @Override
    public long createUniqueId() throws LLZException
    {
        // Id generator is already thread safe
        return this.idGenerator.newId();
    }

    @Override
    public void registerTopicEndPoint(final LLZAutodiscEndPointType endPointType, final LLZAutoDiscTopicEndPoint endPointInfo) throws LLZException
    {
        if (endPointType == LLZAutodiscEndPointType.PUBLISHER)
        {
            this.pubEndPointsPublisher.registerTopicEndPoint(endPointInfo);
        }
        else if (endPointType == LLZAutodiscEndPointType.RESPONDER)
        {
            this.respEndPointsPublisher.registerTopicEndPoint(endPointInfo);
        }
    }

    @Override
    public void unregisterTopicEndPoint(final LLZAutodiscEndPointType endPointType, final long topicUniqueId) throws LLZException
    {
        if (endPointType == LLZAutodiscEndPointType.PUBLISHER)
        {
            this.pubEndPointsPublisher.unregisterTopicEndPoint(topicUniqueId);
        }
        else if (endPointType == LLZAutodiscEndPointType.RESPONDER)
        {
            this.respEndPointsPublisher.unregisterTopicEndPoint(topicUniqueId);
        }
    }

    @Override
    public void subscribeToTopicEndPoints(final String topicName, final LLZAutodiscEndPointType endPointType, final ILLZAutodiscTopicEndPointChangeListener listener) throws LLZException
    {
        if (endPointType == LLZAutodiscEndPointType.PUBLISHER)
        {
            this.pubTypeEndPointsSubscriber.subscribeToTopic(topicName, listener);
        }
        else if (endPointType == LLZAutodiscEndPointType.RESPONDER)
        {
            this.respTypeEndPointsSubscriber.subscribeToTopic(topicName, listener);
        }
    }

    @Override
    public void unsubscribeFromTopicEndPoints(final String topicName, final LLZAutodiscEndPointType endPointType) throws LLZException
    {
        if (endPointType == LLZAutodiscEndPointType.PUBLISHER)
        {
            this.pubTypeEndPointsSubscriber.unsubscribeFromTopic(topicName);
        }
        else if (endPointType == LLZAutodiscEndPointType.RESPONDER)
        {
            this.respTypeEndPointsSubscriber.unsubscribeFromTopic(topicName);
        }
    }

    @Override
    public void stop() throws LLZException
    {
        // Stop both end-point publishers, they will return from their refresh methods
        this.pubEndPointsPublisher.stop();
        this.respEndPointsPublisher.stop();

        // Stop the refresh thread
        this.keepAliveTask.stop();

        // Shutdown the hazelcast instance
        this.hazelcastInstance.shutdown();
    }

    
    /**
     * Thread that keeps alive the Hazelcast instancies 
     * 
     * @author XE52727
     *
     */
    public class EndPointsKeepAliveTask implements Runnable
    {
        /** Refresher Thread stopper */
        private volatile boolean shouldStop = false;
        
        /** True if the refresher thread is already stopped */
        private volatile boolean stopped = false;

        
        @Override
        public void run()
        {           
            // TODO, refactorizar para que se entienda mejor y tenga menos complejidad
            while (!this.shouldStop)
            {
                try
                {
                    // Refresh the end-points
                    LLZHazelcastManager.this.pubEndPointsPublisher.refreshActiveEndpoints();
                    LLZHazelcastManager.this.respEndPointsPublisher.refreshActiveEndpoints();

                    // Wait for some time
                    this.performWait();
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Hazelcast autodiscovery Refresh thread exception [{}]", e);
                }
            }

            this.stopped = true;
        }

        /**
         * 
         * @throws LLZException
         */
        public void stop() throws LLZException
        {
            synchronized (this)
            {
                this.shouldStop = true;
                this.notifyAll();
            }

            try
            {
                while (!this.stopped)
                {
                    Thread.sleep(10);
                }
            }
            catch (final InterruptedException e)
            {
                LOGGER.error("An internal error occurred during stopping Hazelcast autodiscovery Refresh thread [{}]", e);
                throw new LLZException(e);
            }
        }

        /**
         * Executes a busy-waiting
         * 
         * @throws InterruptedException
         */
        private void performWait() throws InterruptedException
        {
            synchronized (this)
            {
                if (!this.shouldStop)
                {
                    // TODO check if this value is correct
                    this.wait(LLZHazelcastManager.this.instanceConfig.getAutodiscoConfig().getRefreshInterval());
                }
            }
        }
    }
}
