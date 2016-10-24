package com.bbva.kyof.vega.unit.protocol;

import com.bbva.kyof.vega.sockets.LLZSocketConstants;
import org.junit.Test;

import java.lang.reflect.Constructor;

/**
 * Created by cnebrera on 26/10/15.
 */
public class LLZSocketConstantsTest
{
    @Test
    public void testConstructor() throws Exception
    {
        Constructor<?>[] cons = LLZSocketConstants.class.getDeclaredConstructors();
        cons[0].setAccessible(true);
        cons[0].newInstance((Object[]) null);
    }
}