package com.bbva.kyof.vega.msg;

/**
 * Implement in order to listen for timeouts on sent requests
 */
public interface ILLZReqTimeoutListener
{
    /**
     * Called when a set request has timed out before being manually closed
     *
     * @param originalSentRequest the original request that has timed out
     */
    void onRequestTimeout(final ILLZSentRequest originalSentRequest);
}
