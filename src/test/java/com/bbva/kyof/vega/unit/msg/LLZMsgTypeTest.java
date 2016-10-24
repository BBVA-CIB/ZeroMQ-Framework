package com.bbva.kyof.vega.unit.msg;

import com.bbva.kyof.vega.msg.LLZMsgType;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Test the LLZMsgTypeTest
 */
public class LLZMsgTypeTest
{
    @Test
    public void testGetByteValue() throws Exception
    {
        Assert.assertEquals(LLZMsgType.DATA.getByteValue(), (byte)0);
        Assert.assertEquals(LLZMsgType.DATA_REQ.getByteValue(), (byte)1);
        Assert.assertEquals(LLZMsgType.DATA_RESP.getByteValue(), (byte)2);
        Assert.assertEquals(LLZMsgType.UNKNOWN.getByteValue(), (byte)127);
    }

    @Test
    public void testFromByte() throws Exception
    {
        Assert.assertEquals(LLZMsgType.DATA, LLZMsgType.fromByte((byte) 0));
        Assert.assertEquals(LLZMsgType.DATA_REQ, LLZMsgType.fromByte((byte) 1));
        Assert.assertEquals(LLZMsgType.DATA_RESP, LLZMsgType.fromByte((byte) 2));
        Assert.assertEquals(LLZMsgType.UNKNOWN, LLZMsgType.fromByte((byte) 8));
    }
}