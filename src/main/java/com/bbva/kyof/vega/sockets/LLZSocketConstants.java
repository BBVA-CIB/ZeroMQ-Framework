package com.bbva.kyof.vega.sockets;

/**
 * Constants for ZMQ Sockets
 */
public final class LLZSocketConstants
{
    /** Value - {@value}. Error launched by Zero MQ where context is called to close session*/
    public static final int ZSOCKET_TERM = 156384765;

    /** Default rate limit for the sockets */
    public static final long DEFAULT_RATE_LIMIT = 1000;

    /**
     * Private constructor to avoid instantiation
     */
    private LLZSocketConstants()
    {
        // Nothing to do
    }
}
