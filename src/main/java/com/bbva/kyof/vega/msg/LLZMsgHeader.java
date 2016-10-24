package com.bbva.kyof.vega.msg;

import java.util.UUID;

/**
 * Represents the header of a framework message
 *
 * <p>This header is added to the messages sent by the framework in order to include additional information
 * that helps to identify sources and receivers and allow special framework messages like security control messages.</p>
 */
public class LLZMsgHeader
{
    /** Internal framework type of the message*/
    private LLZMsgType msgType;

    /** Topic unique ID the message belongs to, the ID is unique per topic name + publisher/requester socket */
    private long topicUniqueId;
  
    /** Identifier of the application instance ID that created the message, it should be unique for the whole cluster */
    private long instanceId;

    /** Framework version of the application instance that created the message */
    private String version;
   
    /** (Optional) Request ID of the message if it is a request or a response */
    private UUID requestId;


    /**
     * Create an empty header, this constructor is only used when the header is read from the binary contents of the message
     */
    public LLZMsgHeader()
    {
        // Nothing to do here
    }

    
    /**
     * Creates a new header provided the header contents
     * 
     * @param msgType internal framework message type
     * @param topicUniqueId topic the message was sent into (internal ID)
     * @param instanceId unique ID of the application that send the message
     * @param version Framework version of the application that send the message
     */
    public LLZMsgHeader(final LLZMsgType msgType,
                        final long topicUniqueId,
                        final long instanceId, 
                        final String version)
    {
        this.msgType = msgType;
        this.topicUniqueId = topicUniqueId;
        this.instanceId = instanceId;
        this.version = version;
    }

    
    /** @return the unique application ID of the application where the header was created */
    public long getInstanceId()
    {
        return this.instanceId;
    }

    /**
     * Set the unique application Id value for the header
     * @param instanceId the application Id to set
     */
    public void setInstanceId(final long instanceId)
    {
        this.instanceId = instanceId;
    }

    /** @return the framework message type */
    public LLZMsgType getMsgType()
    {
        return this.msgType;
    }

    /**
     * Set the framework message type of the header
     *
     * @param msgType the message type to set
     */
    public void setMsgType(final LLZMsgType msgType)
    {
        this.msgType = msgType;
    }

    /** @return Unique identifier of the request, it can be null if the message is not a request or a response */
    public UUID getRequestId()
    {
        return this.requestId;
    }

    
    /**
     * Set the unique request ID in for the message, only use it if the message is a request or a response
     * 
     * @param requestId the request unique identificator
     */
    public void setRequestId(final UUID requestId)
    {
        this.requestId = requestId;
    }

    /** @return the topic the message was sent into (internal ID) */
    public Long getTopicUniqueId()
    {
        return this.topicUniqueId;
    }

    /**
     * Sets the topic the message was sent into
     * 
     * @param topicUniqueId the new topic value
     */
    public void setTopicUniqueId(final Long topicUniqueId)
    {
        this.topicUniqueId = topicUniqueId;
    }

    /** @return the Framework version of the application than send the message  */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * Sets the Framework version of the application that send the message
     * 
     * @param version Framework version of the application that send the message
     */
    public void setVersion(String version)
    {
        this.version = version;
    }
}