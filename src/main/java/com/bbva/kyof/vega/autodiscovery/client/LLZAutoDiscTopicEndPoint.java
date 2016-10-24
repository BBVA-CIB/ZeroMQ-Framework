package com.bbva.kyof.vega.autodiscovery.client;

/**
 * Class which contains autodiscovery information which travels on the network
 * Represents a socket-topic pair
 */
public class LLZAutoDiscTopicEndPoint implements ILLZAutodiscTopicEndPoint
{
    /** End point type (Publisher/Responder) */
    private final LLZAutodiscEndPointType type;

    /** TopicName */
    private final String topicName;

    /** Socket unique Id */
    private final Long socketId;

    /** Represents socket+topic pair and is the key in the Hazelcast table */
    private final Long topicId;

    /** Unique app id  */
    private final Long instanceId;

    /** Unique publisher address */
    private final String bindAddress;

    
    /**
     * Create a new end point object
     * 
     * @param type Type of the auto discovery endpoint
     * @param topicName topicName
     * @param socketId unique socket id
     * @param topicId unique socket+topic pair id
     * @param instanceId unique App id
     * @param bindAddress end point bind address (used as ID of the object)
     */
    public LLZAutoDiscTopicEndPoint(
            LLZAutodiscEndPointType type,
            String topicName,
            Long socketId,
            Long topicId,
            Long instanceId,
            String bindAddress)
    {
        this.type = type;
        this.topicName = topicName;
        this.socketId = socketId;
        this.topicId = topicId;
        this.instanceId = instanceId;
        this.bindAddress = bindAddress;
    }

    @Override
    public String getTopicName()
    {
        return this.topicName;
    }

    @Override
    public Long getSocketId()
    {
        return this.socketId;
    }

    @Override
    public Long getTopicId()
    {
        return this.topicId;
    }

    @Override
    public Long getInstanceId()
    {
        return instanceId;
    }

    @Override
    public String getBindAddress()
    {
        return this.bindAddress;
    }

    @Override
    public LLZAutodiscEndPointType getType()
    {
        return this.type;
    }

    @Override
    public String toString()
    {
        return "LLZAutoDiscTopicEndPoint{" +
                "type=" + type +
                ", topicName='" + topicName + '\'' +
                ", socketId=" + socketId +
                ", topicId=" + topicId +
                ", instanceId=" + instanceId +
                ", bindAddress='" + bindAddress + '\'' +
                '}';
    }
}
