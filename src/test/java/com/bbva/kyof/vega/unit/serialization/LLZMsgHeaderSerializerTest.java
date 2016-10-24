package com.bbva.kyof.vega.unit.serialization;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.serialization.LLZMsgHeaderSerializer;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Test header serialization
 */
public class LLZMsgHeaderSerializerTest
{
    @Test
    public void testConstructor() throws Exception
    {
        Constructor<?>[] cons = LLZMsgHeaderSerializer.class.getDeclaredConstructors();
        cons[0].setAccessible(true);
        cons[0].newInstance((Object[]) null);
    }

    @Test
    public void testSerializeDeserialize() throws Exception
    {
        // Create an String and write it into a buffer
        final String userMessageString = "This is a user message";
        final ByteBuffer userMessage = ByteBuffer.allocate(128);
        LLUSerializerUtils.STRING.write(userMessageString, userMessage);

        // Prepare like if we were going to send it and store the size
        userMessage.flip();
        final int userMsgSize = userMessage.limit();

        // Create the header
        final LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA, -2L, 123L, "2.0");

        // Now write the trailer
        ByteBuffer msgAndHeader1 = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, userMessage);

        // The buffers should be different, it should be using now the thread local buffer
        Assert.assertNotSame(msgAndHeader1, userMessage);

        // Do it again, now the buffers should be the same because the thread local buffer has been allocated already
        ByteBuffer msgAndHeader2 = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, userMessage);
        Assert.assertEquals(msgAndHeader1, msgAndHeader2);

        // Check the new limit
        Assert.assertEquals(msgAndHeader2.limit(), userMsgSize + LLZMsgHeaderSerializer.calculateHeaderSerializedSize(header));

        // Now deserialize
        LLZMsgHeader readedHeader = LLZMsgHeaderSerializer.deserializeHeader(msgAndHeader2);

        // Check the read info
        Assert.assertEquals(readedHeader.getMsgType(), LLZMsgType.DATA);
        Assert.assertEquals(readedHeader.getTopicUniqueId().longValue(), -2L);
        Assert.assertEquals(readedHeader.getInstanceId(), 123L);
        Assert.assertEquals(readedHeader.getVersion(), "2.0");

        // Read the message and check that is correct
        final String decodedStringMsg = LLUSerializerUtils.STRING.read(msgAndHeader2);
        Assert.assertEquals(userMessageString, decodedStringMsg);

        // Finally check that hte position is at the end of the message
        Assert.assertEquals(msgAndHeader2.limit(), msgAndHeader2.position());
    }

    @Test
    public void testSerializeDeserializeBigMessage() throws Exception
    {
        // Simulate a small message
        final ByteBuffer smallMessage = ByteBuffer.allocate(128);
        smallMessage.putInt(1111);
        smallMessage.limit(128);
        smallMessage.position(0);

        // Simulate a big message
        final ByteBuffer bigMessage = ByteBuffer.allocate(3096);
        bigMessage.putInt(2222);
        bigMessage.limit(3096);
        bigMessage.position(0);

        final UUID requestID = UUID.randomUUID();

        // Create the header and serialize
        final LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA, -2L, 123L, "2.0");
        ByteBuffer msgAndHeader1 = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, smallMessage);
        
        // Now serialize the big message and add the request ID
        header.setRequestId(requestID);
        ByteBuffer msgAndHeader2 = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, bigMessage);

        // Since the message is bigger than the initial size for thread local buffer it should be different now
        Assert.assertNotSame(msgAndHeader1, msgAndHeader2);

        // If we do it again now it should be the same
        ByteBuffer msgAndHeader3 = LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, bigMessage);
        Assert.assertEquals(msgAndHeader2, msgAndHeader3);

        // Make sure the original message limits have not been modified
        Assert.assertEquals(bigMessage.limit(), 3096);
        Assert.assertEquals(bigMessage.position(), 0);

        // Check the new limit
        Assert.assertEquals(msgAndHeader2.limit(), bigMessage.limit() + LLZMsgHeaderSerializer.calculateHeaderSerializedSize(header));

        // Deserialize to make sure everything is correct
        LLZMsgHeader readedHeader = LLZMsgHeaderSerializer.deserializeHeader(msgAndHeader2);

        // Check the read info
        Assert.assertEquals(readedHeader.getMsgType(), LLZMsgType.DATA);
        Assert.assertEquals(readedHeader.getTopicUniqueId().longValue(), -2L);
        Assert.assertEquals(readedHeader.getInstanceId(), 123L);
        Assert.assertEquals(readedHeader.getVersion(), "2.0");
        Assert.assertEquals(readedHeader.getRequestId(), requestID);
    }

    @Test
    public void testSerializeDeserializeIntoArray() throws Exception
    {
        // Create the header
        final LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA, -2L, 123L, "2.0");

        // Now write the header into an array
        byte[] headerSerialized = LLZMsgHeaderSerializer.serializeHeader(header);

        // Deserialize
        final LLZMsgHeader deserializedHeader = LLZMsgHeaderSerializer.deserializeHeader(ByteBuffer.wrap(headerSerialized));

        // Check the read info
        Assert.assertEquals(deserializedHeader.getMsgType(), LLZMsgType.DATA);
        Assert.assertEquals(deserializedHeader.getTopicUniqueId().longValue(), -2L);
        Assert.assertEquals(deserializedHeader.getInstanceId(), 123L);
        Assert.assertEquals(deserializedHeader.getVersion(), "2.0");
    }
}