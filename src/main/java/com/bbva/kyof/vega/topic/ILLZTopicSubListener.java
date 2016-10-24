package com.bbva.kyof.vega.topic;

import com.bbva.kyof.vega.msg.ILLZRcvMessage;

/**
 * Implement in order to receive messages on a subscribed topic
 */
public interface ILLZTopicSubListener
{
    /**
     * Method called when a message is received.
     *
     * IMPORTANT: If the received message contents are going to be accessed from a separate thread the message should be promoted!!
     *
     * @param receivedMessage the received message
     */
    void onMessageReceived(final ILLZRcvMessage receivedMessage);
}

