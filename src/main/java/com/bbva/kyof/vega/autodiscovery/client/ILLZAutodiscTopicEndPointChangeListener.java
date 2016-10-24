/**
 * 
 */
package com.bbva.kyof.vega.autodiscovery.client;


/**
 * @author XE52727
 */
public interface ILLZAutodiscTopicEndPointChangeListener
{
    /**
     * Called when a new EndPoint is added
     * 
     * @param autodiscoveryInfo the EndPoint info added
     */
    void onEndPointAdded(final ILLZAutodiscTopicEndPoint autodiscoveryInfo);
    
    /**
     * Called when a EndPoint is removed
     * 
     * @param autodiscoveryInfo the EndPoint info removed
     */
    void onEndPointRemoved(final ILLZAutodiscTopicEndPoint autodiscoveryInfo);
}
