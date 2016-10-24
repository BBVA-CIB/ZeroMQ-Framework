package com.bbva.kyof.vega.functional;

import com.bbva.kyof.vega.msg.ILLZRcvMessage;
import com.bbva.kyof.vega.topic.ILLZTopicSubListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Listener for the testing App
 *
 * Created by XE48745 on 16/09/2015.
 */
public class PubSubTestSubListener implements ILLZTopicSubListener
{
    private final static Logger LOGGER = LoggerFactory.getLogger(PubSubTestSubListener.class);

    private List<ILLZRcvMessage> receivedMessages = new LinkedList<ILLZRcvMessage>();

    @Override
    public void onMessageReceived(final ILLZRcvMessage message)
    {
        message.promote();
        receivedMessages.add(message);

        LOGGER.info("Message received: Topic: [{}]", message.getTopicName());
    }

    public List<ILLZRcvMessage> getReceivedMessages()
    {
        return this.receivedMessages;
    }

    public void clear()
    {
        this.receivedMessages.clear();
    }
}
