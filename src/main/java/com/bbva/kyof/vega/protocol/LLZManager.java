package com.bbva.kyof.vega.protocol;

import java.util.concurrent.atomic.AtomicBoolean;

import com.bbva.kyof.vega.Version;
import com.bbva.kyof.vega.topic.ILLZTopicReqListener;
import com.bbva.kyof.vega.topic.ILLZTopicResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.bbva.kyof.vega.config.LLZInstanceLocalConfigReader;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.topic.ILLZTopicPublisher;
import com.bbva.kyof.vega.topic.ILLZTopicRequester;
import com.bbva.kyof.vega.topic.ILLZTopicSubListener;
import com.bbva.kyof.vega.topic.ILLZTopicSubscriber;
import com.bbva.kyof.vega.util.LibraryPathManager;

/**
 * Main framework class that represent an instance of the communications framework
 */
public final class LLZManager implements ILLZManager
{
    /** Instance of a Logger class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZManager.class);
   
    /** Context of the instance with all the common information */
    private final LLZInstanceContext instanceContext;
    
    /** True if the manager should be stopped */
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    /** The manager to handle all publication logic */
    private final LLZPublishersManager publishersManager;
    
    /** The manager to handle all subscription logic */
    private final LLZSubscribersManager subscribersManager;

    /** The manager to handle all requester logic */
    private final LLZRequestersManager requestersManager;
        
    /** The manager to handle all responder logic */
    private final LLZRespondersManager respondersManager ;

    /**
     * Construct the manager class, it will also create and initialize the ZMQ context
     * 
     * @param parameters An instance of LLZMQParameters to get the parameters needed
     * @param instanceContext The context of the instance
     * @throws LLZException
     */
    private LLZManager(final LLZManagerParams parameters, final LLZInstanceContext instanceContext) throws LLZException
    {
        this.instanceContext = instanceContext;
        
        try
        {
            // Create and set the ZMQ context
            this.instanceContext.setZmqContext(ZMQ.context(parameters.getNumberOfThreads()));
            
            // Start the internal request manager
            this.instanceContext.startRequestManager();
            
            // Start the autodiscovery
            this.instanceContext.startAutodiscovery();

            // Print ZMQ and Framework current versions
            this.printVersions();

            // Initialize the manager for publications
            this.publishersManager = new LLZPublishersManager(this.instanceContext);

            // Initialize the manager for subscriptions
            this.subscribersManager = new LLZSubscribersManager(this.instanceContext);

            // Initialize the manager for requesters
            this.requestersManager = new LLZRequestersManager(this.instanceContext);

            // Initialize the manager for responders
            this.respondersManager = new LLZRespondersManager(this.instanceContext);

        }
        catch (final UnsatisfiedLinkError e)
        {
            LOGGER.error("Cannot find one or more of the ZeroMQ native library (.dll | .so). [{}]", e);

            throw new LLZException("Cannot not find one or more of the ZeroMQ native library", e);
        }
        catch (final ZMQException e)
        {
            LOGGER.error("Unexpected error occurred while creating ZeroMQ native Context. [{}]", e);

            throw new LLZException("An unexpected error occurred while creating ZeroMQ native Context", e);
        }
    }

    /**
     * Prints the current versions of the ZMQ library and Framework
     */
    private void printVersions()
    {
        Version.printZMQVersion();
        Version.printFrameworkVersion();
    }

    /**
     * Create a new instance of a ZeroMQ manager given the initialization parameters
     * 
     * @param parameters the parameters to initialize the manager
     * @return the created instance
     *
     * @throws LLZException exception thrown if there is a problem creating the instance
     */
    public static ILLZManager createInstance(final LLZManagerParams parameters) throws LLZException
    {
        
        LOGGER.info("Creating a new ZMQ manager instance with parameters [{}]", parameters);

        // First load the native libraries if required
        LibraryPathManager.getInstance().loadNativeLibraries(parameters.getZmqLibraryPath());

        // Create the Instance Context 
        final LLZInstanceContext instanceContext = new LLZInstanceContext(parameters);

        // Load and validate the configuration
        LLZManager.validateAndLoadConfiguration(parameters, instanceContext);

        // Now create the instance
        return new LLZManager(parameters, instanceContext);
    }

    
    /**
     * Load and validate the configuration
     *
     * @param parameters instance parameters
     * @param instanceContext the context of the instance
     * @throws LLZException exception thrown if there is any problem
     */
    private static void validateAndLoadConfiguration(final LLZManagerParams parameters, final LLZInstanceContext instanceContext) throws LLZException
    {
        // Load and validate the global instance configuration
        final LLZInstanceLocalConfigReader instanceConfigReader = new LLZInstanceLocalConfigReader(parameters.getInstanceName(), parameters.getConfigurationFile());
        instanceConfigReader.loadAndValidateConfig();

        // Load the configuration into the context
        instanceContext.setInstanceConfiguration(instanceConfigReader.getLoadedConfig());
    }

    @Override
    public ILLZTopicPublisher createPublisher (final String topic) throws LLZException
    {
        LOGGER.info("Creating publisher for topic [{}]", topic);

        return this.publishersManager.createTopicPublisher(topic);
    }

    @Override
    public void destroyPublisher(final String topic) throws LLZException
    {
        LOGGER.info("Destroying publisher for topic [{}]", topic);

        this.publishersManager.destroyTopicPublisher(topic);
    }

    @Override
    public ILLZTopicResponder createResponder(final String topic, final ILLZTopicReqListener requestListener) throws LLZException
    {
        LOGGER.info("Creating responder for topic [{}]", topic);

        return this.respondersManager.createTopicResponder(topic, requestListener);
    }

    @Override
    public void destroyResponder(final String topic) throws LLZException
    {
        LOGGER.info("Destroying responder for topic [{}]", topic);

        this.respondersManager.destroyTopicResponder(topic);
    }

    @Override
    public ILLZTopicRequester createRequester(final String topic) throws LLZException
    {
        LOGGER.info("Creating requester for topic [{}]", topic);

        return this.requestersManager.createTopicRequester(topic);
    }

    @Override
    public void destroyRequester(String topic) throws LLZException
    {
        LOGGER.info("Destroying requester for topic [{}]", topic);

        this.requestersManager.destroyTopicRequester(topic);
    }

    @Override
    public ILLZTopicSubscriber subscribeToTopic(final String topicName, final ILLZTopicSubListener listener) throws LLZException
    {
        LOGGER.info("Subscribing to topic [{}]", topicName);

        return this.subscribersManager.subscribeToTopic(topicName, listener);
    }

    @Override
    public void unsubscribeFromTopic(final String topicName) throws LLZException
    {
        LOGGER.info("Unsubscribing from topic [{}]", topicName);

        this.subscribersManager.unsubscribeFromTopic(topicName);
    }

    @Override
    public void stop() throws LLZException
    {
        LOGGER.info("Stopping the Manager ID [{}]", this.instanceContext.getInstanceUniqueId());

        if (!this.shouldStop.compareAndSet(false, true))
        {
            LOGGER.error("Trying to prepareToStop an already stopped manager");
            throw new LLZException("The manager is already stopped");
        }

        // Close the request manager in the context
        this.instanceContext.stopRequestManager();

        // Close the managers
        this.publishersManager.stop();
        this.subscribersManager.stop();
        this.requestersManager.stop();
        this.respondersManager.stop();

        // Stop the auto discovery mechanism
        this.instanceContext.stopAutodiscovery();

        // Finally close the context, it will automatically term any subscription internal thread or socket
        this.instanceContext.getZmqContext().close();

        LOGGER.info("Managers stopped successfully");
    }

    @Override
    public boolean isRunning()
    {
        return !this.shouldStop.get();
    }
}
