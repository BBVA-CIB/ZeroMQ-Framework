package com.bbva.kyof.vega.autodiscovery.client.hazelcast;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutoDiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.LLZAutodiscEndPointType;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

/**
 * Class which contains autodiscovery information which travels on the network
 * Represents a socket-topic pair 
 */
public class LLZTopicEndPointDAO implements IdentifiedDataSerializable, ILLZAutodiscTopicEndPoint
{
    /** End point type (Publisher/Responder) */
    private LLZAutodiscEndPointType type;

    /** TopicName */
    private String topicName = null;

    /** Socket unique Id */
    private Long socketId = null;

    /** Represents socket+topic pair and is the key in the Hazelcast table */
    private Long topicId = null;

    /** Unique app id  */
    private Long instanceId = null;

    /** Unique publisher address */
    private String bindAddress;


    public LLZTopicEndPointDAO()
    {
    }


    /**
     * New socket description
     * @param topicName topicName
     * @param socketId unique socket id
     * @param socketId unique socket+topic pair id 
     * @param instanceId unique App id
     * @param bindAddress end point bind address (used as ID of the object)
     */
    public LLZTopicEndPointDAO(
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

    /**
     * New socket description
     * @param endPointInfo
     */
    public LLZTopicEndPointDAO(final LLZAutoDiscTopicEndPoint endPointInfo)
    {
        this.type = endPointInfo.getType();
        this.topicName = endPointInfo.getTopicName();
        this.socketId = endPointInfo.getSocketId();
        this.topicId = endPointInfo.getTopicId();
        this.instanceId = endPointInfo.getInstanceId();
        this.bindAddress = endPointInfo.getBindAddress();
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
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        LLZTopicEndPointDAO that = (LLZTopicEndPointDAO) o;

        return this.bindAddress.equals(that.bindAddress);

    }

    @Override
    public int hashCode()
    {
        return this.bindAddress.hashCode();
    }

    @Override
    public int getFactoryId()
    {
        return LLZHazelcastFactory.FACTORY_ID;
    }

    @Override
    public int getId()
    {
        return LLZHazelcastFactory.PUBLISHER_END_POINT_DAO;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException
    {
        out.writeInt(this.type.getIntValue());
        out.writeUTF(this.topicName);
        out.writeLong(this.socketId);
        out.writeLong(this.topicId);
        out.writeLong(this.instanceId);
        out.writeUTF(this.bindAddress);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException
    {
        this.type = LLZAutodiscEndPointType.fromIntValue(in.readInt());
        this.topicName = in.readUTF();
        this.socketId = in.readLong();
        this.topicId = in.readLong();
        this.instanceId = in.readLong();
        this.bindAddress = in.readUTF();
    }

    @Override
    public String toString()
    {
        return "LLZTopicEndPointDAO{" +
                "type='" + type.toString() + '\'' +
                ", topicName='" + topicName + '\'' +
                ", socketId=" + socketId +
                ", topicId=" + topicId +
                ", instanceId=" + instanceId +
                ", bindAddress='" + bindAddress + '\'' +
                '}';
    }
}
