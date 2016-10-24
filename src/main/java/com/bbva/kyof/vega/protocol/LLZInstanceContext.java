package com.bbva.kyof.vega.protocol;

import org.zeromq.ZMQ;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodisc;
import com.bbva.kyof.vega.autodiscovery.client.hazelcast.LLZHazelcastManager;
import com.bbva.kyof.vega.config.LLZInstanceConfigWrapper;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.sockets.LLZAsyncSentRequestManager;

/**
 * Stores the manager instance context with common information that is going to go through
 */
public class LLZInstanceContext
{
    /** The parameters used to initialize the manager */
    private final LLZManagerParams parameters;

    /** The manager to handle timeouts on asynchronous requests */
    private LLZAsyncSentRequestManager requestManager;
    
    /** Instance unique identifier */
    private long instanceUniqueId;

    /** The configuration of the manager instance */
    private LLZInstanceConfigWrapper instanceConfig = null;
   
    /** The ZMQ context */
    private ZMQ.Context zmqContext = null;

    /** Autodiscovery manager*/
    private ILLZAutodisc autodiscoveryManager = null;

    /**
     * Create a new instance context given the manager parameters
     * 
     * @param parameters the manager parameters
     * @throws LLZException 
     */
    public LLZInstanceContext(final LLZManagerParams parameters) throws LLZException
    {
        this.parameters = parameters;
    }

    /** Start the internal request manager */
    public void startRequestManager()
    {
        this.requestManager  = new LLZAsyncSentRequestManager();
    }

    /** Stop the internal request manager */
    public void stopRequestManager()
    {
        this.requestManager.stopAndWaitToFinish();
    }

    /** Start the autodiscoveryManager instance 
     * @throws LLZException */
    public void startAutodiscovery() throws LLZException
    {
        this.autodiscoveryManager = new LLZHazelcastManager(this.instanceConfig);
        this.instanceUniqueId = this.autodiscoveryManager.createUniqueId();

    }
   
    /** Stop the autodiscoveryManager instance 
     *  @throws LLZException  */
    public void stopAutodiscovery() throws LLZException 
    {
        this.autodiscoveryManager.stop();
    }
    
    
    /** @return the instance unique identifier */
    public long getInstanceUniqueId()
    {
        return this.instanceUniqueId;
    }
    
    /** @return the manager instance readed configuration */
    public LLZInstanceConfigWrapper getInstanceConfig()
    {
        return this.instanceConfig;
    }

    /** @return the ZMQ context */
    public ZMQ.Context getZmqContext()
    {
        return this.zmqContext;
    }
    
    /** Set the ZMQ context */
    public void setZmqContext(final ZMQ.Context zmqContext)
    {
        this.zmqContext = zmqContext;
    }

    /** Set the instance configuration */
    public void setInstanceConfiguration(final LLZInstanceConfigWrapper instanceConfiguration)
    {
        this.instanceConfig = instanceConfiguration;
    }
    
    /** @return the parameters used to create the manager */
    public LLZManagerParams getParameters()
    {
        return this.parameters;
    }

    /** @return the Asynchronous request manager */
    public LLZAsyncSentRequestManager getRequestManager()
    {
        return this.requestManager;
    }
    
    /** @return the unique id across all the nodes in the "cluster"
     *  @throws LLZException */   
    public long createUniqueId() throws LLZException
    {
        return this.autodiscoveryManager.createUniqueId();
    }

    /** @return the autodiscovery instance */
    public ILLZAutodisc getAutodiscovery()
    {
        return this.autodiscoveryManager;
    }
    
}
