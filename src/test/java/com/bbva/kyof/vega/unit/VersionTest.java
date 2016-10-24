package com.bbva.kyof.vega.unit;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.Test;
import org.zeromq.ZMQ;

import com.bbva.kyof.vega.Version;

/**
 * Created by XE48745 on 15/09/2015.
 */
public class VersionTest
{
    
    /** Current version of the framework */
    private static final String FRAMEWORK_VERSION_NUMBER = "3.0.0";
    
    /** Current version of the ZMQ library */
    private static final String ZMQ_VERSION_NUMBER = "4.0.4";

    
    @Test
    public void testGetFrameworkVersionNumber() throws Exception
    {
        assertTrue(FRAMEWORK_VERSION_NUMBER.equals(Version.getFrameworkVersionNumber()));
    }

    @Test
    public void testGetZMQVersionNumber() throws Exception
    {
        assertTrue(ZMQ_VERSION_NUMBER.equals(ZMQ.getVersionString()));
    }

    @Test
    public void testConstructor() throws Exception
    {
        Constructor<?>[] cons = Version.class.getDeclaredConstructors();
        cons[0].setAccessible(true);
        cons[0].newInstance((Object[]) null);
    }
}