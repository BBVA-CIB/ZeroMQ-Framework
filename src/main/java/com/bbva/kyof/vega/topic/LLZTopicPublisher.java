package com.bbva.kyof.vega.topic;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.exception.LLZException;

/**
 * The topicName publisher is the class that conglomerates all the functionality to publish messages into a topicName
 *
 * Each publisher belongs to a single topicName and handles the sending of messages to all the "publishers transports" associated to that topicName.
 *
 * The class is thread-safe
 */
public final class LLZTopicPublisher implements ILLZTopicPublisher
{
    /** Instance of a Logger class */
    private static final Logger LOGGER = LoggerFactory.getLogger(LLZTopicPublisher.class);

    /** Topic that is going to send */
    private final String topicName;

    /** Publisher that can send the messages into real transport */
    private final ILLZTopicMsgPublisher topicMsgPublisher;
        
    /** Topic unique Id*/
    private final long topicUniqueId;

    /** Lock for access to the class */
    private final Object lock = new Object();

    /** True if the publisher has been stopped */
    private boolean stopped = false;

    
    /**
     * Constructor of the class
     * 
     * @param topicName Topic name that is going to send
     * @param topicUniqueId Topic Unique Identificator
     * @param publisher The object that does the physical message publication (socket)
     */
    public LLZTopicPublisher(final String topicName, final long topicUniqueId, final ILLZTopicMsgPublisher publisher)
    {
        this.topicName = topicName;
        this.topicUniqueId = topicUniqueId;
        this.topicMsgPublisher = publisher;
    }

    @Override
    public void publish(final ByteBuffer message) throws LLZException
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Sending user message on topicName [{}]", this.topicName);
        }

        synchronized (lock)
        {
            // Make sure the publisher has not been stopped
            if (this.stopped)
            {
                LOGGER.error("Error, trying to send a message on a closed publisher on topicName [{}]", this.topicName);
                throw new LLZException("Trying to publish a message on a closed publisher on topicName " + this.topicName);
            }

            this.topicMsgPublisher.sendMessage(this.topicName, this.topicUniqueId, message);
        }
    }

    @Override
    public String getTopicName()
    {
        return this.topicName;
    }
  

    @Override
    public long getTopicUniqueId()
    {
        return this.topicUniqueId;
    }
      
    
    /**
     * Stops the publisher, it wont let any new message to be published
     */
    public void stop()
    {
        synchronized (lock)
        {
            this.stopped = true;
        }
    }

    @Override
    public boolean isClosed()
    {
        synchronized (this.lock)
        {
            return this.stopped;
        }
    }


}
