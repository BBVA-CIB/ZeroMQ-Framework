package com.bbva.kyof.vega.functional;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.kyof.vega.msg.ILLZRcvResponse;
import com.bbva.kyof.vega.msg.ILLZSentRequest;
import com.bbva.kyof.vega.msg.ILLZTopicRespListener;

/**
 * Listener for the testing App
 *
 * Created by XE48745 on 16/09/2015.
 */
public class RespListener implements ILLZTopicRespListener
{
    private final static Logger LOGGER = LoggerFactory.getLogger(RespListener.class);
    
    private final long responseDelay;
    
    private List<ILLZRcvResponse> receivedResponses = Collections.synchronizedList(new LinkedList<ILLZRcvResponse>());

    
    
    public RespListener(final long responseDelay)
    {
        this.responseDelay = responseDelay;
    }

    public List<ILLZRcvResponse> getReceivedResponses()
    {
        return this.receivedResponses;
    }

    public void clear()
    {
        this.receivedResponses.clear();
    }

    @Override
    public void onResponseReceived(ILLZSentRequest originalSentRequest, ILLZRcvResponse response)
    {
        response.promote();

        // Simulate some delay
        try
        {
            Thread.sleep(this.responseDelay);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        receivedResponses.add(response);

        LOGGER.info("Response received: Topic: [{}], OrigReqID: [{}]", response.getTopicName(), response.getOriginalRequestId());
    }
}
