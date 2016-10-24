package com.bbva.kyof.vega.protocol;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.Version;
import com.bbva.kyof.vega.config.general.RespSocketSchema;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.ILLZRcvReqResponder;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.msg.LLZRcvRequest;
import com.bbva.kyof.vega.serialization.LLZMsgHeaderSerializer;
import com.bbva.kyof.vega.sockets.LLZRespSocket;
import com.bbva.kyof.vega.topic.LLZTopicResponder;
import com.bbva.kyof.vega.util.InterfaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;

import com.bbva.kyof.vega.sockets.ILLZRespSocketReqHandler;

/**
 * The responder manager handles the transport and internal ZMQ sockets to receive requests and publish responses
 */                                                                     
public final class LLZResponder implements ILLZRespSocketReqHandler, ILLZRcvReqResponder
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZResponder.class);

    /** Responder configuration */
    private final RespSocketSchema responderConfigSchema;

    /** Unique identifier of the responder */
    private final long responderUniqueId;

    /** ZMQ protocol handler to receive requests and publish responses */
    private final LLZRespSocket responseSocket;

    /** This Map stores all topic responders associated to this responder socket */
    private final ConcurrentHashMap<Long, LLZTopicResponder> topicRespondersByTopicId = new ConcurrentHashMap<>();

    /** Context of the instance */
    private final LLZInstanceContext instanceContext;

    /** Publisher final full address once it has been bind to a port */
    private final String responderFullAddress;
    
   
    /**
     * Constructor of the class
     * @param instanceContext Context of the instance
     * @param config schema configuration
     * @throws LLZException
     */
    public LLZResponder(final LLZInstanceContext instanceContext, final RespSocketSchema config) throws LLZException
    {
        this.instanceContext = instanceContext;
        this.responderConfigSchema = config;

        LOGGER.debug("Creating responder for schema [{}]", this.responderConfigSchema.getName());

        // Create responder connection string without port
        final String respConnString = this.responderConfigSchema.getTransportMedia() + "://"+
                this.responderConfigSchema.getTransportInterface();

        // Create the full address by using the final selected port from the range, the port is selected by ZMQ on connection
        // and stored inside the publisher on the constructor
        final String responderAddress = InterfaceResolver.resolveBindAddress(respConnString + ":" );

        // Create the responder, it will connect to a random port in the given range
        this.responseSocket = LLZRespSocket.createNewSocket(
                this.instanceContext.getZmqContext(),
                this.responderConfigSchema.getName(),
                responderAddress,
                this,
                this.responderConfigSchema.getMinPort(),
                this.responderConfigSchema.getMaxPort());

        // Add the finally used port by the socket to the address
        this.responderFullAddress = responderAddress + this.responseSocket.getCurrentPort();

        // Create the unique ID for the responder
        this.responderUniqueId = this.instanceContext.createUniqueId();
    }

    /**
     * Adds a new topic responder to receive requests on the topic
     *
     * @param topicUniqueId The unique id of the topic+publisher to subscribe
     * @param topicResponder topic responder for the topic id
     * @throws LLZException exception thrown if already subscribed or if there is a problem subscribing
     */
    public void addTopicResponder(final long topicUniqueId, final LLZTopicResponder topicResponder) throws LLZException
    {
        // Add the topic responder
        this.topicRespondersByTopicId.putIfAbsent(topicUniqueId, topicResponder);
    }

    /**
     * Remove a topic responder for the socket
     *
     * @param topicUniqueId the unique id of the topic responder to remove
     */
    public void removeTopicResponder(final long topicUniqueId)
    {
        this.topicRespondersByTopicId.remove(topicUniqueId);
    }

    /**
     * Stop the manager
     */
    public void stop() throws LLZException
    {
        LOGGER.info("Prepare responder to be stopped");

        // Leave it ready to stop, it wont actually stopped it, that will be done when the context is closed
        this.responseSocket.prepareToStop();
    }

        
    @Override
    public void onSocketReqReceived(final ByteBuffer request, final ZFrame responseAddress)
    {
        // Deserialize the header
        LLZMsgHeader header;
        try
        {
            header = LLZMsgHeaderSerializer.deserializeHeader(request);
        }
        catch (final LLZException | LLUSerializationException e)
        {
            LOGGER.error("Error deserializing received request message", e);
            return;
        }

        // Process differently depending on the message type
        switch (header.getMsgType())
        {
            case DATA_REQ:
                this.processUserRequestReceived(header, request, responseAddress);
                break;
            default:
                LOGGER.warn("Message received of wrong type [{}], expected [{}]",
                        header.getMsgType(), LLZMsgType.DATA_REQ);
                break;
        }
    }

    /**
     * Process the received request coming from a user
     */
    private void processUserRequestReceived(final LLZMsgHeader header, final ByteBuffer requestContents, final ZFrame responseAddress)
    {
        // Find the event listener for the request and process it
        final LLZTopicResponder topicResponder = this.topicRespondersByTopicId.get(header.getTopicUniqueId());

        if (topicResponder != null)
        {
            // Create the request message and provide it to the topic responder
            final LLZRcvRequest requestMessage = new LLZRcvRequest(header, requestContents, responseAddress, this, topicResponder.getTopicName());
            topicResponder.onUserRequestReceived(requestMessage);
        }
    }

   /**
     * Returns string/address of ZMQ socket
     * This is the unique ID for this manager (ip:port combination)
     * @return responder socket
     */
    public String getResponderFullAddress()
    {
        return this.responderFullAddress;
    }

    public Long getResponderUniqueId()
    {
      return this.responderUniqueId;
    }

    @Override
    public void sendReqResponse(
            final long topicId,
            final String topicName,
            final UUID requestId,
            final ByteBuffer responseContent,
            final ZFrame responseAddress) throws LLZException
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Sending response: Type [{}], AppId [{}], Topic [{}]",
                    LLZMsgType.DATA_RESP,
                    this.instanceContext.getInstanceUniqueId(),
                    topicName);
        }

        // Create the response header
        final LLZMsgHeader responseHeader = new LLZMsgHeader(
                LLZMsgType.DATA_RESP,
                topicId,
                this.instanceContext.getInstanceUniqueId(),
                Version.getFrameworkVersionNumber());

        responseHeader.setRequestId(requestId);

        // Create the response message
        final ByteBuffer responseMessage = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(responseHeader, responseContent);

        // Send the response
        this.responseSocket.sendSocketResponse(responseMessage, responseAddress);
    }
}