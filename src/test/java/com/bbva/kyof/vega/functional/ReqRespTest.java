package com.bbva.kyof.vega.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.utils.serialization.model.LLUSerializationException;
import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.msg.ILLZRcvResponse;
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
public class ReqRespTest
{
    private static final String CONFIG_FILE = "functional/reqRespTestConfig.xml";
    private static final String APP_NAME = "TestInstance";
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;
    private ILLZManager manager = null;
    private volatile int cont= 0;
    

    @org.junit.BeforeClass
    public static void init()
    {
        MANAGER_CONFIG_FILE = PubSubTest.class.getClassLoader().getResource(CONFIG_FILE).getPath();
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
            e.printStackTrace();
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
        }
        catch (final LLZException e)
        {
            e.printStackTrace();
            fail("Error creating the LLZManager Instance in the LLZManager test initialization. Error ["+e.getMessage()+"]");
           
        }
    }

    @Test
    public void testReqRespMultipleTopics() throws Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        
        final RespListener respListener1 = new RespListener(0);
        final RespListener respListener2 = new RespListener(0);
        final RespListener respListener3 = new RespListener(0);

        final ReqListener reqListener1 = new ReqListener();
        final ReqListener reqListener2 = new ReqListener();
        final ReqListener reqListener3 = new ReqListener();

        manager.createResponder("TOPIC_1", reqListener1);
        manager.createResponder("TOPIC_2", reqListener2);
        manager.createResponder("TOPIC_3", reqListener3);

        final ILLZTopicRequester requester1 = manager.createRequester("TOPIC_1");
        final ILLZTopicRequester requester2 = manager.createRequester("TOPIC_2");
        final ILLZTopicRequester requester3= manager.createRequester("TOPIC_3");

        // Now send some requests
        final ILLZSentRequest sentReq1 = this.sendRequest(requester1, reusableBuffer, "Msg1", 3000, respListener1, null);
        Thread.sleep(100);
        final ILLZSentRequest sentReq2 = this.sendRequest(requester2, reusableBuffer, "Msg2", 3000, respListener2, null);
        Thread.sleep(100);
        final ILLZSentRequest sentReq3 = this.sendRequest(requester1, reusableBuffer, "Msg3", 3000, respListener1, null);
        Thread.sleep(100);
        final ILLZSentRequest sentReq4 = this.sendRequest(requester2, reusableBuffer, "Msg4", 3000, respListener2, null);
        Thread.sleep(100);
        final ILLZSentRequest sentReq5 = this.sendRequest(requester3, reusableBuffer, "Msg5", 3000, respListener3, null);

        // Wait for the request and responses to arrive
        Thread.sleep(1000);

        // Check all messages information
        List<ILLZRcvResponse> receivedMessagesOnListener1 = respListener1.getReceivedResponses();
        List<ILLZRcvResponse> receivedMessagesOnListener2 = respListener2.getReceivedResponses();
        List<ILLZRcvResponse> receivedMessagesOnListener3 = respListener3.getReceivedResponses();
        
        final int totalReceivedMsg = receivedMessagesOnListener1.size() +
                receivedMessagesOnListener2.size() +
                receivedMessagesOnListener3.size();
        
        Assert.assertEquals("Wrong number of responses received", 5, totalReceivedMsg);

        // Check each message
        this.checkMessage(respListener1.getReceivedResponses().get(0), "Msg1_Resp", sentReq1, "TOPIC_1");
        this.checkMessage(respListener2.getReceivedResponses().get(0), "Msg2_Resp", sentReq2, "TOPIC_2");
        this.checkMessage(respListener1.getReceivedResponses().get(1), "Msg3_Resp", sentReq3, "TOPIC_1");
        this.checkMessage(respListener2.getReceivedResponses().get(1), "Msg4_Resp", sentReq4, "TOPIC_2");
        this.checkMessage(respListener3.getReceivedResponses().get(0), "Msg5_Resp", sentReq5, "TOPIC_3");

        // Check that instance IDs between responders are the same     
        Assert.assertEquals(respListener1.getReceivedResponses().get(0).getInstanceId(),
                             respListener2.getReceivedResponses().get(0).getInstanceId());

        Assert.assertEquals(respListener1.getReceivedResponses().get(0).getInstanceId(),
                             respListener2.getReceivedResponses().get(1).getInstanceId());

        // Check that instance IDs between same topic and responder are the same
        Assert.assertEquals(respListener1.getReceivedResponses().get(0).getInstanceId(),
                            respListener1.getReceivedResponses().get(1).getInstanceId());

        Assert.assertEquals(respListener2.getReceivedResponses().get(0).getInstanceId(),
                            respListener2.getReceivedResponses().get(1).getInstanceId());
    }

    @Test
    public void testDestroyRequester() throws Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        
        final RespListener respListener1 = new RespListener(0);
        final RespListener respListener2 = new RespListener(0);

        final ReqListener reqListener1 = new ReqListener();
        final ReqListener reqListener2 = new ReqListener();
        
        manager.createResponder("TOPIC_1", reqListener1);
        manager.createResponder("TOPIC_2", reqListener2);

        final ILLZTopicRequester requester1 = manager.createRequester("TOPIC_1");
        final ILLZTopicRequester requester2 = manager.createRequester("TOPIC_2");

        // Now send some requests
        this.sendRequest(requester1, reusableBuffer, "Msg1", 3000, respListener1, null);
        Thread.sleep(100);
        this.sendRequest(requester2, reusableBuffer, "Msg2", 3000, respListener2, null);
        Thread.sleep(100);

        // Check they have arrive
        Assert.assertEquals("Wrong number of responses received", 2, respListener1.getReceivedResponses().size() +  respListener2.getReceivedResponses().size());

        // Now destroy one requester, send requests and check that only one has arrived
        manager.destroyRequester("TOPIC_1"); 
        
        respListener1.clear();
        respListener2.clear();

        // Wait
        Thread.sleep(1000);
        
        this.sendRequest(requester1, reusableBuffer, "Msg3", 3000, respListener1, null);
        Thread.sleep(100);
        final ILLZSentRequest sentRequest = this.sendRequest(requester2, reusableBuffer, "Msg4", 3000, respListener2, null);
        Thread.sleep(100);

        Assert.assertEquals("Wrong number of responses received", 1,  respListener1.getReceivedResponses().size() +  respListener2.getReceivedResponses().size());
        this.checkMessage(respListener2.getReceivedResponses().get(0),"Msg4_Resp", sentRequest, "TOPIC_2");

    }

    
    @Test
    public void testTimeouts() throws Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final ReqListener reqListener = new ReqListener();
        final TimeoutListener timeoutListener = new TimeoutListener();
        final RespListener respListener = new RespListener(100);
                 
        manager.createResponder("TOPIC_1", reqListener);
        
        final ILLZTopicRequester requester1 = manager.createRequester("TOPIC_1");

        // Wait for the connections to be ready
        Thread.sleep(1000);

        // Now send some requests
        this.sendRequest(requester1, reusableBuffer, "Msg1", 300, respListener, timeoutListener);
        this.sendRequest(requester1, reusableBuffer, "Msg2", 300, respListener, timeoutListener);

        // Wait for the request and responses to arrive
        Thread.sleep(1000);

        // We should have 2 timeouts and 2 responses, the request have not been closed, the response should have arrived
        Assert.assertTrue(timeoutListener.getTimeoutRequests().size() == 2);
        Assert.assertTrue(respListener.getReceivedResponses().size() == 2);

        // Clear
        timeoutListener.clear();
        respListener.clear();

        // Now send 2 requests and close only the first one
        this.sendRequest(requester1, reusableBuffer, "Msg3", 300, respListener, timeoutListener).closeRequest();
        this.sendRequest(requester1, reusableBuffer, "Msg4", 300, respListener, timeoutListener);

        // Wait for the request and responses to arrive
        Thread.sleep(1000);

        // We should have 1 response and 1 timeout
        Assert.assertTrue(timeoutListener.getTimeoutRequests().size() == 1);
        Assert.assertTrue(respListener.getReceivedResponses().size() == 1);
    }
    
    @Test 
    public void testStress() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final int numElements = 1000;

        List<RespListener> respListenersList = new ArrayList<>();
        ILLZTopicRequester[] requestersArray = new ILLZTopicRequester[numElements];
        ILLZSentRequest[] sentRequestArray = new ILLZSentRequest[numElements];
      
        for (int i = 0; i < numElements / 2; ++i)
        {
            ReqListener reqListener1 = new ReqListener();
            manager.createResponder("TOPIC_" + i, reqListener1);
        }
        
        for (int i = numElements / 2; i < numElements; ++i)
        {
            ReqListener reqListener2 = new ReqListener();
            manager.createResponder("XX_TOPIC_" + i, reqListener2);
        }
             
        for (int i = 0; i < numElements / 2; ++i)
        {
            RespListener respListener1 = new RespListener(0);
            requestersArray[i] = manager.createRequester("TOPIC_" + i);
            respListenersList.add(respListener1);
        }
       
        for (int i = numElements / 2; i < numElements; ++i)
        {
            RespListener respListener2 = new RespListener(0);
            requestersArray[i] = manager.createRequester("XX_TOPIC_" + i);
            respListenersList.add(respListener2);
        }
        
        // Wait for the connections to be ready
        Thread.sleep(10000);

        // Now send some requests
        for (int i = 0; i < numElements / 2; ++i)
        {
            RespListener respListener1 = respListenersList.get(i);     
            sentRequestArray[i] = this.sendRequest(requestersArray[i], reusableBuffer, "Msg" + i, 3000, respListener1, null);
        }
       
        for (int i = numElements / 2; i < numElements; ++i)
        {
            RespListener respListener2 = respListenersList.get(i);     
            sentRequestArray[i] = this.sendRequest(requestersArray[i], reusableBuffer, "Msg" + i, 3000, respListener2, null);  
        }
        
        // Wait messages to have arrived
        Thread.sleep(30000);
        
        // Count total received messages on topic [TOPIC_ + i]
        int totalReceivedMsgOnListener1 = 0;  

        for (int i = 0; i < numElements / 2; ++i)
        {
            RespListener listener1 = respListenersList.get(i);
            totalReceivedMsgOnListener1 += listener1.getReceivedResponses().size();
            this.checkMessage(listener1.getReceivedResponses().get(0), "Msg" + i + "_Resp", sentRequestArray[i], "TOPIC_" + i);
        }
                
        // Count total received messages on topic [XX_TOPIC_ + i]
        int totalReceivedMsgOnListener2 = 0;  
        for (int i = numElements / 2; i < numElements; ++i)
        {
            RespListener listener2 = respListenersList.get(i);
            totalReceivedMsgOnListener2 += listener2.getReceivedResponses().size();
            this.checkMessage(listener2.getReceivedResponses().get(0), "Msg" + i + "_Resp", sentRequestArray[i], "XX_TOPIC_" + i);
        }

        assertEquals(numElements / 2, totalReceivedMsgOnListener1);
        assertEquals(numElements / 2, totalReceivedMsgOnListener2);
    }
   
    
    @Test 
    public void testStressTwo() throws java.lang.Exception
    {
        final ByteBuffer reusableBuffer = ByteBuffer.allocate(1024);
        final int numElements = 1000;

        final RespListener respListener = new RespListener(0);
        ILLZTopicRequester[] requestersArray = new ILLZTopicRequester[numElements];
        ILLZSentRequest[] sentRequestArray = new ILLZSentRequest[numElements];
      
        for (int i = 0; i < numElements / 2; ++i)
        {
            ReqListener reqListener = new ReqListener();
            manager.createResponder("TOPIC_" + i, reqListener);
        }
        
        for (int i = numElements / 2; i < numElements; ++i)
        {
            ReqListener reqListener = new ReqListener();
            manager.createResponder("XX_TOPIC_" + i, reqListener);
        }
             
        for (int i = 0; i < numElements / 2; ++i)
        {
            requestersArray[i] = manager.createRequester("TOPIC_" + i);
            Thread.sleep(1000);
        }
       
        for (int i = numElements / 2; i < numElements; ++i)
        {
            requestersArray[i] = manager.createRequester("XX_TOPIC_" + i);
        }
        
        // Wait for the connections to be ready
        Thread.sleep(10000);

        // Now send some requests
        for (int i = 0; i < numElements; ++i)
        {
            sentRequestArray[i] = this.sendRequest(requestersArray[i], reusableBuffer, "Msg" + i, 3000, respListener, null);
        }
                
        this.cont=0;

        // We keep counting while new messages are coming
        while (respListener.getReceivedResponses().size() > this.cont || respListener.getReceivedResponses().size() == 0)
        {
            this.cont = respListener.getReceivedResponses().size(); 
            Thread.sleep(1000);
        }
  
        assertEquals(numElements, this.cont);
    }
   


    private void checkMessage(ILLZRcvResponse rcvMessage, String msgStringContent, ILLZSentRequest sentReq, String topic) throws LLUSerializationException
    {
        Assert.assertEquals("Wrong requestId", rcvMessage.getOriginalRequestId(), sentReq.getRequestId());
        Assert.assertEquals("Wrong topic received", topic, rcvMessage.getTopicName());
        Assert.assertEquals("Wrong message contents", msgStringContent, LLUSerializerUtils.STRING.read(rcvMessage.getMessageContent()));
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
}