package com.bbva.kyof.vega.multiinstance;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.msg.ILLZRcvMessage;
import com.bbva.kyof.vega.topic.ILLZTopicSubListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Listener for the testing App
 *
 * Created by XE48745 on 16/09/2015.
 */
public class AutoDiscoveryListener implements ILLZTopicSubListener
{
    private final static Logger LOGGER = LoggerFactory.getLogger(AutoDiscoveryListener.class);

    private List<ILLZRcvMessage> receivedMessages = Collections
            .synchronizedList(new LinkedList<ILLZRcvMessage>());
    private String name;

    public AutoDiscoveryListener(String name)
    {
        this.name = name;
    }

    @Override
    public void onMessageReceived(final ILLZRcvMessage message)
    {
        message.promote();
        receivedMessages.add(message);

        String s="";
        try
        {
            s=LLUSerializerUtils.STRING.read(message.getMessageContent());
        }
        catch (LLUSerializationException e)
        {
            e.printStackTrace();
        }
        LOGGER.info("Message received: Listener name [{}] Topic: [{}] Message: [{}]", name, message.getTopicName() , s);
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
