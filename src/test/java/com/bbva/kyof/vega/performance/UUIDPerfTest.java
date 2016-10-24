package com.bbva.kyof.vega.performance;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

/**
 * Created by cnebrera on 03/12/15.
 */
public class UUIDPerfTest
{
    private static final int NUM_TESTS = 100000;
    private static final Logger LOGGER = LoggerFactory.getLogger(UUIDPerfTest.class);
    private final Random rnd = new Random(System.currentTimeMillis());

    @Test
    public void UUIDRandomCreationTimeTest()
    {
        testrandomUUIDTime(true);
        testrandomUUIDTime(false);
    }

    @Test
    public void UUIDRandomReqIdCreationTimeTest()
    {
        testRequestIDUUIDTime(true);
        testRequestIDUUIDTime(false);
    }

    public void testRequestIDUUIDTime(final boolean warmUp)
    {
        final long startTime = System.nanoTime();

        for (int i = 0; i < NUM_TESTS; i++)
        {
            new UUID(rnd.nextLong(), rnd.nextLong());
        }

        final long endTime = System.nanoTime();

        if (!warmUp)
        {
            LOGGER.info("Random UUID ReqId Avg Creation Time: [{}]", (endTime - startTime) / NUM_TESTS);
        }
    }

    public void testrandomUUIDTime(final boolean warmUp)
    {
        final long startTime = System.nanoTime();

        for (int i = 0; i < NUM_TESTS; i++)
        {
            UUID.randomUUID();
        }

        final long endTime = System.nanoTime();

        if (!warmUp)
        {
            LOGGER.info("Random UUID Avg Creation Time: [{}]", (endTime - startTime) / NUM_TESTS);
        }
    }
}
