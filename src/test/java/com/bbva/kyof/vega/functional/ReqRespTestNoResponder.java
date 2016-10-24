package com.bbva.kyof.vega.functional;

import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.ILLZReqTimeoutListener;
import com.bbva.kyof.vega.msg.ILLZSentRequest;
import com.bbva.kyof.vega.msg.ILLZTopicRespListener;
import com.bbva.kyof.vega.protocol.ILLZManager;
import com.bbva.kyof.vega.protocol.LLZManager;
import com.bbva.kyof.vega.protocol.LLZManagerParams;
import com.bbva.kyof.vega.topic.ILLZTopicRequester;

/**
 * Test for the {@link LLZManager} class
 * Created by XE48745 on 15/09/2015.
 */
public class ReqRespTestNoResponder implements ILLZReqTimeoutListener
{
    private static final String CONFIG_FILE = "functional/reqRespTestNoResponder.xml";
    private static final String APP_NAME = "TestInstance";
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;
    private AtomicInteger numberOfTimeouts = new AtomicInteger(0);
    private ILLZManager manager = null;

    @org.junit.BeforeClass
    public static void init()
    {
        MANAGER_CONFIG_FILE = ReqRespTestNoResponder.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(APP_NAME, MANAGER_CONFIG_FILE).build();
    }

    @Before
    public void beforeTest()
    {
        // Create a new manager instance before each test
        try
        {
            manager = LLZManager.createInstance(MANAGER_PARAMS);
        }
        catch (final LLZException e)
        {
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
        }
    }

    @After
    public void afterTest() throws Exception
    {
        // Stop the manager instance after each test and wait a bit to release the sockets
        try
        {
            manager.stop();
            Thread.sleep(3000);
            this.numberOfTimeouts.set(0);
        }
        catch (final LLZException e)
        {
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
        }
    }

    @Test
    public void testTimeout() throws Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final RespListener respListener = new RespListener(0);
 
        final ILLZTopicRequester requester1 = manager.createRequester("TOPIC_1");
        final ILLZTopicRequester requester2 = manager.createRequester("TOPIC_2");

        // Wait for the connections to be ready
        Thread.sleep(2000);

        // Now send some requests
        this.sendRequest(requester1, reusableBuffer, "Msg1", 1000, respListener, this);
        this.sendRequest(requester2, reusableBuffer, "Msg2", 1000, respListener, this);

        // Wait for the timeouts to fire
        Thread.sleep(2000);

        // Check they have arrive
        Assert.assertEquals(this.numberOfTimeouts.get(), 2);
    }


    private ILLZSentRequest sendRequest(final ILLZTopicRequester requester,
                                        final ByteBuffer buffer,
                                        final String msg,
                                        final long timeout,
                                        final ILLZTopicRespListener respListener,
                                        final ILLZReqTimeoutListener timeoutListener) throws LLUSerializationException, LLZException
    {
        buffer.clear();
        LLUSerializerUtils.STRING.write(msg, buffer);
        buffer.flip();

        return requester.sendRequest(buffer, timeout, respListener, timeoutListener);
    }

    @Override
    public void onRequestTimeout(ILLZSentRequest originalSentRequest)
    {
        this.numberOfTimeouts.getAndIncrement();
    }
}