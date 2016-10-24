package com.bbva.kyof.vega.autodiscovery.client.hazelcast.processors;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import com.bbva.kyof.vega.autodiscovery.client.ILLZAutodiscTopicEndPoint;
import com.bbva.kyof.vega.autodiscovery.client.hazelcast.LLZHazelcastFactory;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.query.Predicate;

/**
 * Class which is executed for each entry on the cache and selects only the ones which match a pattern
 */
public class TopicRegexpEndPointPredicate implements Predicate<Long, ILLZAutodiscTopicEndPoint>,
        IdentifiedDataSerializable
{
    /** TopicName Regexp */
    private String topicRegexp;

    /** Compiled pattern */
    private Pattern pattern;

    /** Empty constructor for hazelcast serializer */
    public TopicRegexpEndPointPredicate()
    {
        // Nothing to do
    }

    /**
     * Constructor with the topic regexp
     *
     * @param topicRegexp regular expresion to match against the topic name
     */
    public TopicRegexpEndPointPredicate(final String topicRegexp)
    {
        this.topicRegexp = topicRegexp;
        this.pattern = Pattern.compile(topicRegexp);
    }

    @Override
    public boolean apply(final Map.Entry<Long, ILLZAutodiscTopicEndPoint> entry)
    {
        return (this.pattern.matcher(entry.getValue().getTopicName()).matches());
    }

    @Override
    public int getFactoryId()
    {
        return LLZHazelcastFactory.FACTORY_ID;
    }

    @Override
    public int getId()
    {
        return LLZHazelcastFactory.TOPIC_REGEXP_END_POINT_PREDICATE;
    }

    @Override
    public void writeData(final ObjectDataOutput objectDataOutput)
            throws IOException
    {
        objectDataOutput.writeUTF(this.topicRegexp);
    }

    @Override
    public void readData(final ObjectDataInput objectDataInput)
            throws IOException
    {
        this.topicRegexp = objectDataInput.readUTF();
        this.pattern = Pattern.compile(this.topicRegexp);
    }
}