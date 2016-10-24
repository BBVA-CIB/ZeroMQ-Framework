package com.bbva.kyof.vega.protocol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.bbva.kyof.vega.config.general.RespSocketSchema;
import com.bbva.kyof.vega.exception.LLZExceptionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.exception.LLZException;

/**
 * Created by cnebrera on 01/04/16.
 */
public class LLZRespondersPool
{
    /** Logger of the class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZPublishersPools.class);

    /** Set with all the created responders in the pools */
    private final Set<LLZResponder> createdResponders = new HashSet<>();

    /** Map with all the pools of publishers by configuration schema name */
    private final Map<String, LinkedList<LLZResponder>> poolBySchemaName = new HashMap<>();

    /** Context of the instance with all the common information */
    private final LLZInstanceContext instanceContext;

    /**
     * Constructor
     *
     * @param instanceContext context of the instance
     */
    public LLZRespondersPool(final LLZInstanceContext instanceContext)
    {
        this.instanceContext = instanceContext;
    }

    /**
     * Gets or creates a responder from the pool.
     *
     * @return the created or returned publisher
     * @throws LLZException if no port available in range and no reusable sockets found
     */
    public LLZResponder getOrCreateResponder(final RespSocketSchema socketSchema) throws LLZException
    {
        // Get or create the pool to use based on the schema configuration
        LinkedList<LLZResponder> respondersPool = this.poolBySchemaName.get(socketSchema.getName());

        if (respondersPool == null)
        {
            respondersPool = new LinkedList<>();
            this.poolBySchemaName.put(socketSchema.getName(), respondersPool);
        }

        // If the all the possible publishers for the pool have been created, return the head and move it to the end
        if (respondersPool.size() == socketSchema.getMaxNumPorts())
        {
            return this.getNextResponderInPool(respondersPool);
        }

        try
        {
            // Create a new publisher, add it to the pool and to the map of created publishers
            final LLZResponder newResponder = new LLZResponder(this.instanceContext, socketSchema);
            respondersPool.add(newResponder);
            this.createdResponders.add(newResponder);
            return newResponder;
        }
        catch (final LLZException e)
        {
            // If the problem is that there are no ports available try to use an existing publisher
            if (e.getExceptionCode() == LLZExceptionCode.NO_AVAILABLE_PORTS)
            {
                if (!respondersPool.isEmpty())
                {
                    return this.getNextResponderInPool(respondersPool);
                }
                else
                {
                    LOGGER.error("There is no available port in range [{}]-[{}] and there are no reusable sockets found", socketSchema.getMinPort(), socketSchema.getMaxPort());
                    throw new LLZException("No port available in range and no reusable sockets found", e);
                }
            }
            else
            {
                LOGGER.error("Unexpected error creating responder", e);
                throw e;
            }
        }
    }

    /**
     * Stop all the internal publishers and clean the internal information
     */
    public void stopAndCleanAll() throws LLZException
    {
        // Stop all the publishers
        for (final LLZResponder responder : this.createdResponders)
        {
            responder.stop();
        }

        // Clean the internal information
        this.createdResponders.clear();
        this.poolBySchemaName.clear();
    }

    /** @return the next publisher in the pool */
    private LLZResponder getNextResponderInPool(final LinkedList<LLZResponder> pool)
    {
        if (pool.isEmpty())
        {
            return null;
        }
        else
        {
            final LLZResponder result = pool.removeFirst();
            pool.addLast(result);
            return result;
        }
    }
}
