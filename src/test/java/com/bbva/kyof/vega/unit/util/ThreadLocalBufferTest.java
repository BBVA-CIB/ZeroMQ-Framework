package com.bbva.kyof.vega.unit.util;

import com.bbva.kyof.vega.util.ThreadLocalBuffer;
import junit.framework.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Created by cnebrera on 03/12/15.
 */
public class ThreadLocalBufferTest
{
    @Test
    public void testGetBuffer() throws Exception
    {
        final ThreadLocalBuffer threadLocalBuffer = new ThreadLocalBuffer();

        final ByteBuffer smallBuffer = threadLocalBuffer.getBuffer(128);

        // Try again
        ByteBuffer currentBuffer = threadLocalBuffer.getBuffer(128);

        Assert.assertEquals(smallBuffer, currentBuffer);

        // Now require a bigger buffer
        final ByteBuffer mediumBuffer = threadLocalBuffer.getBuffer(2000);

        Assert.assertNotSame(currentBuffer, mediumBuffer);

        // Try again
        currentBuffer = threadLocalBuffer.getBuffer(2000);
        Assert.assertEquals(mediumBuffer, currentBuffer);

        // Force it to grow again
        final ByteBuffer bigBuffer = threadLocalBuffer.getBuffer(6048);
        Assert.assertNotSame(currentBuffer, bigBuffer);

        // Try again
        currentBuffer = threadLocalBuffer.getBuffer(6048);
        Assert.assertEquals(bigBuffer, currentBuffer);
    }
}