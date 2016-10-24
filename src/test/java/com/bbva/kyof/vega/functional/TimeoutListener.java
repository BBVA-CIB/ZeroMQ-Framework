package com.bbva.kyof.vega.functional;

import com.bbva.kyof.vega.msg.ILLZReqTimeoutListener;
import com.bbva.kyof.vega.msg.ILLZSentRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cnebrera on 15/12/15.
 */
public class TimeoutListener implements ILLZReqTimeoutListener
{
    private List<ILLZSentRequest> timeoutRequests = new ArrayList<ILLZSentRequest>();

    @Override
    public void onRequestTimeout(final ILLZSentRequest originalSentRequest)
    {
        this.timeoutRequests.add(originalSentRequest);
    }

    public List<ILLZSentRequest> getTimeoutRequests()
    {
        return timeoutRequests;
    }

    public void clear()
    {
        this.timeoutRequests.clear();
    }
}
