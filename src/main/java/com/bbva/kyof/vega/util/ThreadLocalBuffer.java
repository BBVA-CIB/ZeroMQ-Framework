package com.bbva.kyof.vega.util;

import java.nio.ByteBuffer;

/**
 * This is a helper class to handle thread local buffers.
 *
 * The idea is to simplify the process of memory reuse by using thread local buffers when possible.
 */
public final class ThreadLocalBuffer
{
    /** Initial size of the internal reusable buffer, it will be 1K */
    private static final int INITIAL_REUSABLE_BUFFER_SIZE = 1024;

    /** Grew factor for the buffer if it is not big enough */
    private static final int BUFFER_GROW_FACTOR = 2;

    /** Thread local buffer that will be used to force buffer reuse */
    private final ThreadLocal<ByteBuffer> reusableBuffer = new ThreadLocal<>();

    /**
     * Prepare the thread local buffer to make sure it contains enough space for the required size
     *
     * @param requiredSize required size for the reusable buffer
     * @return the reusable buffer with the required size and clean and ready to use
     */
    public ByteBuffer getBuffer(final int requiredSize)
    {
        // Get the reusable buffer
        ByteBuffer resultBuffer = reusableBuffer.get();

        // Check if the provided buffer is big enough
        if (resultBuffer == null || resultBuffer.capacity() < requiredSize)
        {
            if (requiredSize > INITIAL_REUSABLE_BUFFER_SIZE)
            {
                resultBuffer = ByteBuffer.allocate(requiredSize * BUFFER_GROW_FACTOR);
            }
            else
            {
                resultBuffer = ByteBuffer.allocate(INITIAL_REUSABLE_BUFFER_SIZE);
            }

            reusableBuffer.set(resultBuffer);
        }

        // Clear the result buffer
        resultBuffer.clear();

        return resultBuffer;
    }
}
