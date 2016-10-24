package com.bbva.kyof.vega.unit;

import junit.framework.Assert;

import com.bbva.kyof.vega.protocol.LLZManagerParams;


/**
 * Testing methods for the {@link LLZManagerParams}
 *
 * Created by XE48745 on 19/08/2015.
 */
public class LLZManagerParamsTest
{
    LLZManagerParams params = null;

    @org.junit.Before
    public void setUp() throws Exception
    {
      
        params = new LLZManagerParams.Builder("test", "confFile.txt")
                .numberOfThreads(2)
                .zmqLibraryPath("zmq.dll").build();
    }

    @org.junit.Test
    public void checkToStringMethod() throws Exception
    {
        Assert.assertNotNull(params.toString());
    }

    @org.junit.Test
    public void testGetters() throws Exception
    {
        Assert.assertEquals(params.getConfigurationFile(), "confFile.txt");
        Assert.assertEquals(params.getInstanceName(), "test");
    }

    @org.junit.Test
    public void getOptionals() throws Exception
    {
        Assert.assertEquals(params.getNumberOfThreads(), 2);
        Assert.assertEquals(params.getZmqLibraryPath(), "zmq.dll");
    }
}