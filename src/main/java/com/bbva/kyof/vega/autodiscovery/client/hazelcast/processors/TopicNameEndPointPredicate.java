package com.bbva.kyof.vega.autodiscovery.client.hazelcast.processors;

import java.io.IOException;
import java.util.Map;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.hazelcast.LLZHazelcastFactory;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.query.Predicate;

/**
 * Class which is executed for each entry on the cache and selects only the ones which match a pattern
 */
public class TopicNameEndPointPredicate implements Predicate<Long, ILLZAutodiscTopicEndPoint>, IdentifiedDataSerializable
{
    /** TopicName */
    private String topicName;

    /** Empty constructor for hazelcast serializer */
    public TopicNameEndPointPredicate()
    {
    }

    /**
     * Constructor with the topic name
     *
     * @param topicName
     */
    public TopicNameEndPointPredicate(final String topicName)
    {
        this.topicName = topicName;
    }

    @Override
    public boolean apply(final Map.Entry<Long, ILLZAutodiscTopicEndPoint> entry)
    {
        return this.topicName.equals(entry.getValue().getTopicName());
    }

    @Override
    public int getFactoryId()
    {
        return LLZHazelcastFactory.FACTORY_ID;
    }

    @Override
    public int getId()
    {
        return LLZHazelcastFactory.TOPIC_NAME_END_POINT_PREDICATE;
    }

    @Override
    public void writeData(final ObjectDataOutput objectDataOutput)
            throws IOException
    {
        objectDataOutput.writeUTF(this.topicName);
    }

    @Override
    public void readData(final ObjectDataInput objectDataInput)
            throws IOException
    {
        this.topicName = objectDataInput.readUTF();
    }
}