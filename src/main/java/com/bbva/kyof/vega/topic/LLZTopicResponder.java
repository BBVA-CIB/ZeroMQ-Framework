package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.msg.LLZRcvRequest;

/**
 * The topicName responder is the class that conglomerates all the functionality to responde to incomming request into a topicName
 *
 * Each responder belongs to a single topicName and handles the sending of messages to all the "requesters transports" associated to that topicName.
 *
 * The class is thread-safe
 */
public final class LLZTopicResponder implements ILLZTopicResponder
{
    /** Topic that is going to send */
    private final String topicName;
    
    /** Topic unique Id*/
    private final long topicUniqueId;

    /** User listener for incomming requests */
    private final ILLZTopicReqListener requestListener;

    /** Lock for access to the class */
    private final Object lock = new Object();

    /** True if the responder has been stopped */
    private boolean stopped = false;

    /**
     * Constructor of the class
     *
     * @param topicName Topic that is going to send
     * @param topicUniqueId Unique id of the topic
     * @param requestListener user request listener
     */
    public LLZTopicResponder(final String topicName, final long topicUniqueId, final ILLZTopicReqListener requestListener)
    {
        this.topicName = topicName;
        this.requestListener = requestListener;
        this.topicUniqueId = topicUniqueId;
    }

   
    @Override
    public String getTopicName()
    {
        return this.topicName;
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

    public long getTopicUniqueId()
    {
        return this.topicUniqueId;
    }

    /**
     * Process a user request received
     *
     * @param request the received request
     */
    public void onUserRequestReceived(final LLZRcvRequest request)
    {
        // Send the request to the user request listener to be processed
        this.requestListener.onRequestReceived(request);
    }
}
