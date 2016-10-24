package com.bbva.kyof.vega.unit.exception;

import com.bbva.kyof.vega.exception.LLZException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Testing for {@link LLZException}
 * Created by XE48745 on 16/09/2015.
 */
public class LLZExceptionTest
{
    @Test
    public void testConstructor1()
    {
        final LLZException exception = new LLZException("Socket not created. You must to create the context first, for create them.");

        assertEquals(exception.getMessage(), "Socket not created. You must to create the context first, for create them.");
    }
    @Test
    public void testConstructor2()
    {
        try
        {
            throw new NullPointerException("Exception!!!");
        }
        catch(final java.lang.Exception e)
        {
            final LLZException exception = new LLZException(e);
            assertEquals(exception.getCause(), e);
        }

    }
    @Test
    public void testConstructor3()
    {
        try
        {
            throw new NullPointerException("Exception!!!");
        }
        catch(final java.lang.Exception e)
        {
            final LLZException exception = new LLZException("Exception message for testing.", e);
            assertEquals(exception.getMessage(), "Exception message for testing.");
            assertEquals(exception.getCause(), e);
        }

    }
}