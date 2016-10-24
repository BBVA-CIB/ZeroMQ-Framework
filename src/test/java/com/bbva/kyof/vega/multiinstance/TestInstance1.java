package com.bbva.kyof.vega.multiinstance;

import java.nio.ByteBuffer;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.protocol.ILLZManager;
import com.bbva.kyof.vega.protocol.LLZManager;
import com.bbva.kyof.vega.protocol.LLZManagerParams;
import com.bbva.kyof.vega.topic.ILLZTopicPublisher;

/**
 * Created by XE46274 on 03/02/2016.
 */
public class TestInstance1
{
    private static final String CONFIG_FILE = "multiinstance/autodiscoveryClient1.xml";
    private static final String INSTANCE_NAME = "TestInstance";
    private static ILLZManager manager = null;
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;

    public static void main(String[] args) throws Exception
    {
 
        MANAGER_CONFIG_FILE = TestInstance1.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(INSTANCE_NAME, MANAGER_CONFIG_FILE).build();
        ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
        // Create a new manager instance before each test
        manager = LLZManager.createInstance(MANAGER_PARAMS);

        final ILLZTopicPublisher publisher1 = manager.createPublisher("TOPIC_1");
        final ILLZTopicPublisher publisher2 = manager.createPublisher("TOPIC_2");
        final ILLZTopicPublisher publisher3 = manager.createPublisher("TOPIC_3");
        final ILLZTopicPublisher publisher4 = manager.createPublisher("TOPIC_4X");
        final ILLZTopicPublisher publisher5 = manager.createPublisher("TOPIC_4Y");
        Thread.sleep(5000);

        // Now send some messages
        sendMessage(publisher1, reusableBuffer, "Msg1");
        sendMessage(publisher2, reusableBuffer, "Msg2");
        sendMessage(publisher3, reusableBuffer, "Msg3");
        sendMessage(publisher4, reusableBuffer, "Msg4");
        sendMessage(publisher5, reusableBuffer, "Msg5");
        Thread.sleep(2000);

        manager.stop();
        // Wait for the connections to be ready
    }


    private static int sendMessage(final ILLZTopicPublisher publisher, final ByteBuffer buffer, final String msg) throws
            LLUSerializationException, LLZException
    {
        buffer.clear();
        LLUSerializerUtils.STRING.write(msg, buffer);
        buffer.flip();

        final int msgSize = buffer.limit();
        publisher.publish(buffer);

        return msgSize;
    }
}
