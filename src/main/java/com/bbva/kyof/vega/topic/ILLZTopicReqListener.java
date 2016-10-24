package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.msg.ILLZRcvRequest;

/**
 * Implement in order to receive requests
 */
public interface ILLZTopicReqListener
{
    /**
     * Method to implement in order to receive requests in a topic subscriber.
     *
     * IMPORTANT: If the request message contents are going to be accessed from a separate thread the message should be promoted!!
     *
     * @param request The received request
     */
    void onRequestReceived(final ILLZRcvRequest request);
}

