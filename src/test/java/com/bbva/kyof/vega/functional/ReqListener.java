package com.bbva.kyof.vega.functional;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.utils.serialization.bytebuffer.LLUSerializerUtils;
import com.bbva.kyof.vega.msg.ILLZRcvRequest;
import com.bbva.kyof.vega.topic.ILLZTopicReqListener;

/**
 * Listener for the testing App
 *
 * Created by XE48745 on 16/09/2015.
 */
public class ReqListener implements ILLZTopicReqListener
{
    private final static Logger LOGGER = LoggerFactory.getLogger(ReqListener.class);
    private final ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
    private List<ILLZRcvRequest> receivedRequests = Collections.synchronizedList(new LinkedList<ILLZRcvRequest>());

    
    
    public List<ILLZRcvRequest> getReceivedRequests()
    {
        return this.receivedRequests;
    }

    public void clear()
    {
        this.receivedRequests.clear();
    }


    @Override
    public void onRequestReceived(ILLZRcvRequest receivedRequest)
    {
        LOGGER.info("Request received: Topic: [{}], ReqID: [{}]", receivedRequest.getTopicName(), receivedRequest.getRequestId());
        receivedRequests.add(receivedRequest);

        // Get the request message
        try
        {
            // Get the original message
            String requestMessage = LLUSerializerUtils.STRING.read(receivedRequest.getMessageContent());

            // Prepare the response
            responseBuffer.clear();

            LLUSerializerUtils.STRING.write(requestMessage + "_Resp", responseBuffer);
            responseBuffer.flip();
          
            // Send the response
            receivedRequest.sendResponse(responseBuffer);    
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

  
}
