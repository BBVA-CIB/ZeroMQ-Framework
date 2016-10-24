package com.bbva.kyof.vega.sockets;

import java.nio.ByteBuffer;

/**
 * Interface to implement in order to process responses from a ZMQ request socket
 */
public interface ILLZReqSocketRespHandler
{
    /**
     * Called when a new response arrives into the ZMQ request socket
     *
     * @param response byte buffer containing the response
     */
    void onSocketRespReceived(final ByteBuffer response);
}
