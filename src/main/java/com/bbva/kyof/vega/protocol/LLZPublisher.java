package com.bbva.kyof.vega.protocol;

import java.nio.ByteBuffer;

import com.bbva.kyof.vega.Version;
import com.bbva.kyof.vega.util.InterfaceResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.config.general.PubSocketSchema;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.serialization.LLZMsgHeaderSerializer;
import com.bbva.kyof.vega.sockets.LLZPubSocket;
import com.bbva.kyof.vega.topic.ILLZTopicMsgPublisher;

/**
 * This class handlers a publisher transport with support to publish messages
 */
public class LLZPublisher implements ILLZTopicMsgPublisher
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZPublisher.class);

    /** Publisher configuration */
    private final PubSocketSchema publisherConfigSchema;

    /** Unique identifier of the publisher */
    private final long publisherUniqueId;

    /** Manager instance context */
    private final LLZInstanceContext instanceContext;
	    
    /** ZMQ protocol handler to publish real time messages */
    private final LLZPubSocket publisherSocket;

    /** Object to perform the locking operations */
    private final Object lock = new Object();

    /** Publisher final full address once it has been bind to a port */
    private final String publisherFullAddress;
    
    /** True if the socket has been stopped */
    private boolean stopped = false;

    
    /**
     * Constructor
     * 
     * @param instanceContext  Context of the instance
     * @param config Publisher Socket Schema configuration
     * @throws LLZException if the socket is already in use or if there is an unexpected problem
     */
    public LLZPublisher(final LLZInstanceContext instanceContext, final PubSocketSchema config) throws LLZException
    {
        this.instanceContext = instanceContext;

        this.publisherConfigSchema = config;

        LOGGER.debug("Creating publisher for schema [{}]", this.publisherConfigSchema.getName());

        // Create publisher connection string without port
        final String pubConnString = this.publisherConfigSchema.getTransportMedia() + "://"+
                this.publisherConfigSchema.getTransportInterface();

        // Create the full address by using the final selected port from the range, the port is selected by ZMQ on connection
        // and stored inside the publisher on the constructor
        final String publisherAddress = InterfaceResolver.resolveBindAddress(pubConnString+ ":" );
        
        
        // Create the publisher, it will connect to a random port in the given range
        this.publisherSocket = new LLZPubSocket(this.instanceContext.getZmqContext(),
                                                this.publisherConfigSchema.getName(),
                                                publisherAddress,
                                                this.publisherConfigSchema.getPubRateLimit(),
                                                this.publisherConfigSchema.getMinPort(),
                                                this.publisherConfigSchema.getMaxPort());
        
        this.publisherFullAddress = publisherAddress + this.publisherSocket.getCurrentPort();
        
        // Create the unique ID for the publisher
        this.publisherUniqueId = this.instanceContext.createUniqueId();
    }


    /**
     * Stop the manager and internal sockets
     *
     * @throws LLZException exception thrown if already stopped
     */
    public void stop() throws LLZException
    {
        LOGGER.info("Stopping publisher manager");

        synchronized (this.lock)
        {
            if (this.stopped)
            {
                LOGGER.error("Trying to prepareToStop the publisher manager [{}] but is already stopped", this.publisherConfigSchema.getName());
                throw new LLZException("Cannot prepareToStop an already stopped manager");
            }

            // Now stop the internal socket
            this.publisherSocket.stopAndClose();
            this.stopped = true;
        }
    }

    @Override
    public void sendMessage(final String topic, final long topicPublisherUniqueId, final ByteBuffer message) throws LLZException
    {
        synchronized (this.lock)
        {
            if (this.stopped)
            {
                LOGGER.error("Trying to send a message on a the stopped publisher manager [{}]", this.publisherConfigSchema.getName());
                throw new LLZException("Trying to send a message on an stopped publisher manager");
            }
        
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Sending message: Type [{}], AppId [{}], Topic [{}], PublisherId [{}], Socket [{}]", 
                             LLZMsgType.DATA, this.instanceContext.getInstanceUniqueId(), 
                             topic, 
                             this.publisherUniqueId, 
                             this.publisherFullAddress);
            }
            
            final LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA,
                                                         topicPublisherUniqueId,
                                                         this.instanceContext.getInstanceUniqueId(),
                                                         Version.getFrameworkVersionNumber());
            
            final ByteBuffer messageToPublish = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, message);
            
            this.publisherSocket.send(messageToPublish);
        }
    }
    
    /** @return socket publisher address */
    public String getPublisherFullAddress()
    {
        return this.publisherFullAddress;
    }

    /** @return unique publisher identifier */
    public long getPublisherUniqueId()
    {
        return this.publisherUniqueId;
    }
}
