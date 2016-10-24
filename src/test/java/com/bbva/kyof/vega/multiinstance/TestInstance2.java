package com.bbva.kyof.vega.multiinstance;

import com.bbva.kyof.vega.protocol.ILLZManager;
import com.bbva.kyof.vega.protocol.LLZManager;
import com.bbva.kyof.vega.protocol.LLZManagerParams;

/**
 * Created by XE46274 on 03/02/2016.
 */
public class TestInstance2
{
    private static final String CONFIG_FILE = "multiinstance/autodiscoveryClient2.xml";
    private static final String INSTANCE_NAME = "TestInstance";
    private static ILLZManager manager = null;
    private static LLZManagerParams MANAGER_PARAMS;
    private static String MANAGER_CONFIG_FILE;

    public static void main(String[] args) throws Exception
    {

        MANAGER_CONFIG_FILE = TestInstance2.class.getClassLoader().getResource(CONFIG_FILE).getPath();
        MANAGER_PARAMS = new LLZManagerParams.Builder(INSTANCE_NAME, MANAGER_CONFIG_FILE).build();

        System.setProperty("hazelcast.local.localAddress", "127.0.0.1");
        // Create a new manager instance before each test
        manager = LLZManager.createInstance(MANAGER_PARAMS);

        final AutoDiscoveryListener listener = new AutoDiscoveryListener("listener1");
        final AutoDiscoveryListener listener2 = new AutoDiscoveryListener("listener2");
        final AutoDiscoveryListener listener3 = new AutoDiscoveryListener("listener3");
        
        manager.subscribeToTopic("TOP.*", listener);
        manager.subscribeToTopic("TOP.*X", listener2);
        manager.subscribeToTopic("TOP.*_..", listener3);

        for (int i = 0; i < 5000; ++i)
        {
            System.err.println("Iter: " + i + " Recibidos " + listener.getReceivedMessages().size() + " mensajes en listener1 (esperamos 5*n senders)");
            System.err.println("Iter: " + i + " Recibidos " + listener2.getReceivedMessages().size() + " mensajes en listener2 (esperamos 1*n senders)");
            System.err.println("Iter: " + i + " Recibidos " + listener3.getReceivedMessages().size() + " mensajes en listener3 (esperamos 2*n senders)");

            try
            {
                // Wait for the connections to be ready
                Thread.sleep(10000);
            } catch (Exception e) {}
        }


        manager.stop();
    }
}
