package com.bbva.kyof.vega.unit.msg;

import java.util.Random;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;

import com.bbva.kyof.vega.msg.ILLZRcvResponse;
import com.bbva.kyof.vega.msg.ILLZReqTimeoutListener;
import com.bbva.kyof.vega.msg.ILLZSentRequest;
import com.bbva.kyof.vega.msg.ILLZTopicRespListener;
import com.bbva.kyof.vega.msg.LLZRcvResponse;
import com.bbva.kyof.vega.msg.LLZSentRequest;

/**
 * Created by cnebrera on 03/12/15.
 */
public class LLZSentRequestTest
{
    private final Random rndGenerator = new Random(System.currentTimeMillis());

    @Test
    public void testSimpleMethods() throws Exception
    {
        final SimpleListener listener = new SimpleListener();
        final LLZSentRequest sentRequest = new LLZSentRequest("Topic", 100, listener, listener, rndGenerator);

        Assert.assertEquals(sentRequest.getTopic(), "Topic");
        Assert.assertNotNull(sentRequest.getRequestId());

        // Test number of responses
        Assert.assertEquals(sentRequest.getNumberOfResponses(), 0);
        // It should not have expired
        Assert.assertFalse(sentRequest.hasExpired());

        // Wait for a bit and check again
        Thread.sleep(200);
        Assert.assertTrue(sentRequest.hasExpired());

        // Should not be closed
        Assert.assertFalse(sentRequest.isClosed());

        // Now it should
        sentRequest.closeRequest();
        Assert.assertTrue(sentRequest.isClosed());
    }

    @Test
    public void testNoExpiration() throws Exception
    {
        final SimpleListener listener = new SimpleListener();
        final LLZSentRequest sentRequest = new LLZSentRequest("Topic", 0, listener, listener, rndGenerator);

        // Should not expire
        Thread.sleep(200);
        Assert.assertFalse(sentRequest.hasExpired());
    }

    @Test
    public void testResponseReceived() throws Exception
    {
        final SimpleListener listener = new SimpleListener();
        final LLZSentRequest sentRequest = new LLZSentRequest("Topic", 100, listener, listener, rndGenerator);

        final LLZRcvResponse receivedResponse = EasyMock.createMock(LLZRcvResponse.class);
        sentRequest.onResponseReceived(receivedResponse);

        // Test number of responses
        Assert.assertEquals(sentRequest.getNumberOfResponses(), 1);
        Assert.assertEquals(listener.getNumResponses(), 1);

        // Check the response
        Assert.assertTrue(receivedResponse == listener.getLastReceivedResponse());

        // Send another one
        sentRequest.onResponseReceived(receivedResponse);

        // Test number of responses
        Assert.assertEquals(sentRequest.getNumberOfResponses(), 2);
        Assert.assertEquals(listener.getNumResponses(), 2);

        // Force a timeout
        sentRequest.onRequestTimeout();
        Assert.assertTrue(listener.isHasTimedOut());

        // Now close the request
        sentRequest.closeRequest();

        // The methods of receive request or timeout shouldn't do anything now, reset and test again
        listener.reset();
        sentRequest.onResponseReceived(receivedResponse);
        Assert.assertEquals(sentRequest.getNumberOfResponses(), 2);
        Assert.assertEquals(listener.getNumResponses(), 0);

        sentRequest.onRequestTimeout();
        Assert.assertFalse(listener.isHasTimedOut());
    }

    @Test
    public void testUserException()
    {
        final WrontListener listener = new WrontListener();
        final LLZSentRequest sentRequest = new LLZSentRequest("Topic", 100, listener, listener, rndGenerator);

        final LLZRcvResponse receivedResponse = EasyMock.createMock(LLZRcvResponse.class);

        sentRequest.onResponseReceived(receivedResponse);

        // It should have launched an exception that is captured by the request
        Assert.assertEquals(sentRequest.getNumberOfResponses(), 1);
    }

    @Test
    public void testTimeoutException()
    {
        final WrontListener listener = new WrontListener();
        final LLZSentRequest sentRequest = new LLZSentRequest("Topic", 100, listener, listener, rndGenerator);
        sentRequest.onRequestTimeout();
    }

    public class SimpleListener implements ILLZReqTimeoutListener, ILLZTopicRespListener
    {
        private ILLZRcvResponse lastReceivedResponse = null;
        private int numResponses = 0;
        private boolean hasTimedOut = false;

        @Override
        public void onRequestTimeout(ILLZSentRequest originalSentRequest)
        {
            this.hasTimedOut = true;
        }

        @Override
        public void onResponseReceived(ILLZSentRequest originalSentRequest, ILLZRcvResponse response)
        {
            this.lastReceivedResponse = response;
            this.numResponses++;
        }

        public void reset()
        {
            this.lastReceivedResponse = null;
            this.numResponses = 0;
            this.hasTimedOut = false;
        }

        public ILLZRcvResponse getLastReceivedResponse()
        {
            return lastReceivedResponse;
        }

        public int getNumResponses()
        {
            return numResponses;
        }

        public boolean isHasTimedOut()
        {
            return hasTimedOut;
        }
    }

    public class WrontListener extends SimpleListener
    {
        @Override
        public void onRequestTimeout(ILLZSentRequest originalSentRequest)
        {
            throw new NullPointerException("Null pointer");
        }

        @Override
        public void onResponseReceived(ILLZSentRequest originalSentRequest, ILLZRcvResponse response)
        {
            throw new NullPointerException("Null pointer");
        }
    }
}