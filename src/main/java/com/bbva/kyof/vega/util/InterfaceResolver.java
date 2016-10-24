package com.bbva.kyof.vega.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Helper class to find the right interface value for end-points. If the value given is a "*" it will perform 3 steps:
 *
 * 1- Use the given address if it is not an "*"
 * 2- Try to use the value provided in the zmqframework.localhost java parameter.
 * 3- Get the first found interface address
 */
public class InterfaceResolver
{
    /** System property to set the zmq framework localhost if not provided in configuration */
    public static final String ZMQFRAMEWORK_LOCALHOST_PROP = "zmqframework.localhost";

    /** Local IP address to use, the one in the zmqframework.localhost property or the first one found */
    private static final String LOCAL_IP = InterfaceResolver.findLocalIp();

    /**
     * Return the end point address (keeping the port). It will use the original one or if an "*" is provided it will
     * try to resolve it. (See class description)
     *
     * @param bindAddress the address to resolve
     * @return the resolved address
     */
    public static String resolveBindAddress(final String bindAddress)
    {
        String result = bindAddress;
        if (result.contains("*"))
        {
            String realIp = LOCAL_IP;
            result = result.replaceAll("\\*", realIp);
        }

        return result;
    }

    /**
     * Returns the current IP of the machine so subscribers can connect to us
     * Tries to detect the IP automatically, can be overriden with
     * -Dzmqframework.localhost=xxx.xxx.xxx.xxx
     *
     * @return IP of the machine which will be published to subscribers
     */
    private static String findLocalIp()
    {
        final String LOCAL_IP_VALUE = System.getProperty(ZMQFRAMEWORK_LOCALHOST_PROP, null);

        try
        {
            if (LOCAL_IP_VALUE != null)
            {
                return LOCAL_IP_VALUE;
            }
            else
            {
                // TODO, this really works?
                return InetAddress.getLocalHost().getHostAddress();
            }
        }
        catch (final UnknownHostException e)
        {
            return LOCAL_IP_VALUE;
        }
    }
}
