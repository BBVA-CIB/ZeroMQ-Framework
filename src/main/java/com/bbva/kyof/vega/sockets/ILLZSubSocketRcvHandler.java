package com.bbva.kyof.vega.sockets;

import java.nio.ByteBuffer;

/**
 * Interface to implement in order to receive messages coming directly from a ZMQ SUB socket
 */
public interface ILLZSubSocketRcvHandler
{
    /**
     * Method called when a new message is received from the socket
     *
     * @param message byte buffer containing the received message contents
     */
    void onSocketMsgReceived(final ByteBuffer message);
}
