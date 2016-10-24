package com.bbva.kyof.vega.protocol;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.Version;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.serialization.LLZMsgHeaderSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.config.general.ReqSocketSchema;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.sockets.ILLZReqSocketRespHandler;
import com.bbva.kyof.vega.sockets.LLZReqSocket;
import com.bbva.kyof.vega.topic.ILLZTopicRequestSender;

/**
 * This class handlers a requester transport with support to send requests
 */
public class LLZRequester implements ILLZReqSocketRespHandler, ILLZTopicRequestSender
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZRequester.class);

    /** Publisher configuration */
    private final ReqSocketSchema requesterConfig;

    /** Manager instance context */
    private final LLZInstanceContext instanceContext;
    
    /** ZMQ protocol handler to send requests and receive responses */
    private final LLZReqSocket requestSocket;

    /** Socket addr which identifies this manager */
    private final String reqConnection;

    /** Set with all the topic ids of end-points associated with this requester */
    private final Set<Long> associatedTopicIds = new HashSet<>();

    /**
     * 
     * @param instanceContext Context of the instance
     * @param configSchema configuration squema
     * @param reqConnection req where to connect
     * @throws LLZException
     */
    public LLZRequester(final LLZInstanceContext instanceContext, final ReqSocketSchema configSchema, final String reqConnection) throws LLZException
    {
        this.reqConnection= reqConnection;
        this.instanceContext = instanceContext;
        this.requesterConfig = configSchema;

        LOGGER.debug("Creating requester manager for schema [{}]", this.requesterConfig.getName());

        // Connect and start the requester socket
        this.requestSocket = LLZReqSocket.createNewSocket(
                instanceContext.getZmqContext(),
                this.reqConnection,
                this.requesterConfig.getName(), 
                this);
    }
    
 
    /**
     * Stop the manager and internal sockets
     *
     * @throws LLZException exception thrown if already stopped
     */
    public void stop() throws LLZException
    {
        LOGGER.info("Stopping requester socket");

        try
        {
            this.requestSocket.stop();
        }
        catch (final InterruptedException e)
        {
            LOGGER.error("Thread interrupted while trying to stop the socket", e);
            throw new LLZException("Thread interrupted while trying to stop the socket", e);
        }
    }

  
    @Override
    public void onSocketRespReceived(final ByteBuffer response)
    {
        // Create the received message
        LLZMsgHeader messageHeader;
        try
        {
            messageHeader = LLZMsgHeaderSerializer.deserializeHeader(response);

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Response received of type [{}], on requester [{}]", messageHeader.getMsgType(), this.reqConnection);
            }

            switch (messageHeader.getMsgType())
            {
                case DATA_RESP:
                    this.processUserDataResponse(messageHeader, response);
                    break;
                default:
                    LOGGER.warn("Response received of wrong type [{}], expected [{}]", messageHeader.getMsgType(), LLZMsgType.DATA_RESP);
                    break;
            }
        }
        catch (final LLZException | LLUSerializationException e)
        {
            LOGGER.error("Error deserializing received response on requester " + this.reqConnection, e);
            return;
        }
    }

    private void processUserDataResponse(final LLZMsgHeader messageHeader, final ByteBuffer response)
    {
        // Give it to the response manager
        this.instanceContext.getRequestManager().onResponseReceived(messageHeader, response);
    }
 
   
    @Override
    public void sendTopicRequest(final long topicId, final UUID reqId, final ByteBuffer messageContents) throws LLZException
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Sending request: Type [{}], AppId [{}], TopicId [{}], ReqId [{}]",
                    LLZMsgType.DATA_REQ,
                    this.instanceContext.getInstanceUniqueId(), topicId, reqId);
        }

        // Create the header
        final LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA_REQ, topicId, this.instanceContext.getInstanceUniqueId(), Version.getFrameworkVersionNumber());
        header.setRequestId(reqId);

        // Create the final message
        final ByteBuffer message = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, messageContents);

        // Send the request
        this.requestSocket.sendRequest(message);
    }

    public void addTopicEndPoint(final long topicId)
    {
        this.associatedTopicIds.add(topicId);
    }

    public boolean removeTopicEndPoint(final long topicId)
    {
        this.associatedTopicIds.remove(topicId);
        return this.associatedTopicIds.isEmpty();
    }
}