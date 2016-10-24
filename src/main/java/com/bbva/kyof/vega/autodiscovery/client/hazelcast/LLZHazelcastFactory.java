package com.bbva.kyof.vega.autodiscovery.client.hazelcast;

import com.bbva.kyof.vega.autodiscovery.client.hazelcast.processors.TopicNameEndPointPredicate;
import com.bbva.kyof.vega.autodiscovery.client.hazelcast.processors.TopicRegexpEndPointPredicate;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

/**
 * Created by XE46274 on 03/02/2016.
 */
public class LLZHazelcastFactory implements DataSerializableFactory
{
    /** Factory ID */
    public static final int FACTORY_ID = 1;

    /** Class identifiers */
    public static final int TOPIC_REGEXP_END_POINT_PREDICATE = 0;
    public static final int TOPIC_NAME_END_POINT_PREDICATE = 1;
    public static final int PUBLISHER_END_POINT_DAO = 2;
    public static final int RESPONDER_END_POINT_DAO = 3;

    @Override
    public IdentifiedDataSerializable create(final int typeId)
    {
        switch (typeId)
        {
            case TOPIC_REGEXP_END_POINT_PREDICATE:
                return new TopicRegexpEndPointPredicate();
            case TOPIC_NAME_END_POINT_PREDICATE:
                return new TopicNameEndPointPredicate();
            case PUBLISHER_END_POINT_DAO:
                return new LLZTopicEndPointDAO();
            case RESPONDER_END_POINT_DAO:
                return new LLZTopicEndPointDAO();
            default:
                return null;
        }
    }
}
