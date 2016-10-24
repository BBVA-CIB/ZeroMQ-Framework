package com.bbva.kyof.vega;

import org.zeromq.ZMQ;

/**
 * This class contains information about the version of the framework
 */
public final class Version
{
    /** Current version of the framework */
    private static final String FRAMEWORK_VERSION_NUMBER = "3.0.0";

    /** Hided constructor since it is an utility class */
    private Version()
    {
        // Empty constructor
    }

    /** Returns the current version of the framework */
    public static String getFrameworkVersionNumber()
    {
        return FRAMEWORK_VERSION_NUMBER;
    }


    /** Prints the current version of the framework */
    public static void printFrameworkVersion()
    {
        System.out.println(String.format("Framework Version: %s", FRAMEWORK_VERSION_NUMBER));
    }

    /** Prints the current version of the ZMQ library */
    public static void printZMQVersion()
    {
        System.out.println(String.format("ZMQ Version: %s", ZMQ.getVersionString()));
    }
}
