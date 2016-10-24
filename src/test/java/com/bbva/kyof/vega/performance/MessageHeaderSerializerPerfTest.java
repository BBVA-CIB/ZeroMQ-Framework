package com.bbva.kyof.vega.performance;

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.LLZMsgHeader;
import com.bbva.kyof.vega.msg.LLZMsgType;
import com.bbva.kyof.vega.serialization.LLZMsgHeaderSerializer;

/**
 * Created by cnebrera on 03/12/15.
 */
public class MessageHeaderSerializerPerfTest
{
    private static final int NUM_TESTS = 100000;
    private static final long APP_ID = 238472897463L;
    private static final long TOPIC_ID = 2L;
    private static final String ZMQ_VERSION = "2.0";
    private static final Random RND = new Random(System.currentTimeMillis());
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHeaderSerializerPerfTest.class);

    @Test
    public void test128Msg() throws LLZException
    {
        final ByteBuffer msgContents = this.getNewRndByteBuffer(128);
        testHeaderSerialization(true, msgContents);
        testHeaderSerialization(true, msgContents);
        testHeaderSerialization(true, msgContents);
        testHeaderSerialization(false, msgContents);
    }

    @Test
    public void test256Msg() throws LLZException
    {
        final ByteBuffer msgContents = this.getNewRndByteBuffer(256);
        testHeaderSerialization(true, msgContents);
        testHeaderSerialization(false, msgContents);
    }

    @Test
    public void test512Msg() throws LLZException
    {
        final ByteBuffer msgContents = this.getNewRndByteBuffer(512);
        testHeaderSerialization(true, msgContents);
        testHeaderSerialization(false, msgContents);
    }

    @Test
    public void test1024Msg() throws LLZException
    {
        final ByteBuffer msgContents = this.getNewRndByteBuffer(1024);
        testHeaderSerialization(true, msgContents);
        testHeaderSerialization(false, msgContents);
    }

    private void testHeaderSerialization(final boolean warmUp, final ByteBuffer msgContents) throws LLZException
    {
        final long startTime = System.nanoTime();

        for (int i = 0; i < NUM_TESTS; i++)
        {
            LLZMsgHeader header = new LLZMsgHeader(LLZMsgType.DATA, TOPIC_ID, APP_ID, ZMQ_VERSION);
            LLZMsgHeaderSerializer.serializeHeaderAndMsgIntoReusableBuffer(header, msgContents);
            msgContents.position(0);
            msgContents.limit(msgContents.capacity());
        }

        final long endTime = System.nanoTime();

        if (!warmUp)
        {
            LOGGER.info("Header serialization for msg size [{}] avg time [{}]", msgContents.capacity(), (endTime - startTime) / NUM_TESTS);
        }
    }

    private ByteBuffer getNewRndByteBuffer(final int length)
    {
        final ByteBuffer result = ByteBuffer.allocate(length);
        RND.nextBytes(result.array());
        result.position(0);
        result.limit(result.capacity());

        return result;
    }
}
