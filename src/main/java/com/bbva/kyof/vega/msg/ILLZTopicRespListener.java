package com.bbva.kyof.vega.msg;

/**
 * Implement in order to receive responses to previously sent requests
 */
public interface ILLZTopicRespListener
{
    /**
     * Method called when a response is received for a sent request.
     *
     * IMPORTANT: If the response message contents are going to be accessed from a separate thread the message should be promoted!!
     *
     * @param originalSentRequest original sent request related to the response
     * @param response the received response
     */
    void onResponseReceived(final ILLZSentRequest originalSentRequest, final ILLZRcvResponse response);
}

