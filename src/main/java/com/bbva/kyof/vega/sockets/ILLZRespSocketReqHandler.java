package com.bbva.kyof.vega.sockets;

import org.zeromq.ZFrame;

import java.nio.ByteBuffer;

/**
 * Interface to implement in order to process received requests from a ZMQ socket
 */
public interface ILLZRespSocketReqHandler
{
    /**
     * Method called by the socket when a new request is received
     *
     * @param message the ByteBuffer containing the received request
     * @param responseAddress the address the response should be sent to
     */
    void onSocketReqReceived(final ByteBuffer message, final ZFrame responseAddress);
}
