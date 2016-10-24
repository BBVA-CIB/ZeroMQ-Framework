package com.bbva.kyof.vega.unit;

/**
 * Created by cnebrera on 29/10/15.
 */

import com.bbva.kyof.vega.serialization.UUIDSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

//
//Asynchronous client-to-server (DEALER to ROUTER)
//
//While this example runs in a single process, that is just to make
//it easier to start and stop the example. Each task has its own
//context and conceptually acts as a separate process.

public class AsyncClientServerTest
{
    private final static Logger LOGGER = LoggerFactory.getLogger(AsyncClientServerTest.class);

    //---------------------------------------------------------------------
    //This is our client task
    //It connects to the server, and then sends a request once per second
    //It collects responses as they arrive, and it prints them out. We will
    //run several client tasks in parallel, each with a different random ID.

    private static class Client
    {
        private final AtomicInteger requestId = new AtomicInteger(0);
        final ZMQ.Context zmqContext;
        final Socket client;

        private final UUID uniqueClientId;

        public Client(final UUID uniqueClientId)
        {
            this.uniqueClientId = uniqueClientId;
            this.zmqContext = ZMQ.context(1);

            this.client = zmqContext.socket(ZMQ.DEALER);
            client.setIdentity(UUIDSerializer.uniqueIdToByteArray(uniqueClientId));
            client.connect("tcp://localhost:5570");
        }

        public void start()
        {
            final Thread receiver = new Thread()
            {
                public void run()
                {
                    final PollItem[] items = new PollItem[]{new PollItem(client, Poller.POLLIN)};

                    while (!Thread.currentThread().isInterrupted())
                    {
                        ZMQ.poll(items, 1);

                        if (items[0].isReadable())
                        {
                            ZMsg msg = ZMsg.recvMsg(client);
                            byte[] last = msg.getLast().getData();

                            LOGGER.info("Client [{}]: Response received [{}]", uniqueClientId, new String(last));
                            msg.destroy();
                        }
                    }
                }
            };

            receiver.start();

            final Thread sender = new Thread()
            {
                public void run()
                {
                    while (true)
                    {
                        int requestIdSent = requestId.getAndIncrement();
                        client.send("" + uniqueClientId + ";" + requestIdSent, 0);
                        LOGGER.info("Client [{}], request [{}] sent", uniqueClientId, requestIdSent);

                        try
                        {
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            };

            sender.start();
        }
    }

    //This is our server task.
    //It uses the multithreaded server general to deal requests out to a pool
    //of workers and route replies back to clients. One worker can handle
    //one request at a time but one client can talk to multiple workers at
    //once.

    private static class Server implements Runnable
    {
        public void run()
        {
            ZMQ.Context ctx = ZMQ.context(1);

            //  Frontend protocol talks to clients over TCP
            Socket frontend = ctx.socket(ZMQ.ROUTER);
            frontend.bind("tcp://*:5570");

            //  Backend protocol talks to workers over inproc
            Socket backend = ctx.socket(ZMQ.DEALER);
            UUID inprocID = UUID.randomUUID();
            backend.bind("inproc://" + inprocID);

            //  Launch pool of worker threads, precise number is not critical
            for (int threadNbr = 0; threadNbr < 5; threadNbr++)
            {
                new Thread(new server_worker(ctx, inprocID)).start();
            }

            //  Connect backend to frontend via a proxy
            ZMQ.proxy(frontend, backend, null);

            ctx.close();
        }
    }

    //Each worker task works on one request at a time and sends a random number
    //of replies back, with random delays between replies:

    private static class server_worker implements Runnable
    {
        private ZMQ.Context ctx;
        private final UUID inprocId;

        public server_worker(ZMQ.Context ctx, final UUID inprocId) {
            this.ctx = ctx;
            this.inprocId = inprocId;
        }

        public void run()
        {
            Socket worker = ctx.socket(ZMQ.DEALER);
            worker.connect("inproc://" + inprocId.toString());

            while (!Thread.currentThread().isInterrupted())
            {
                //  The DEALER protocol gives us the address envelope and message
                ZMsg msg = ZMsg.recvMsg(worker);
                ZFrame address = msg.pop();
                ZFrame content = msg.pop();
                assert (content != null);
                msg.destroy();

                LOGGER.info("Req received for id: [{}] and content: [{}]", new String(address.getData()), new String(content.getData()));

                // Send the reply in a separate thread with a delay
                ResponderThread responseThread = new ResponderThread(address, worker);
                responseThread.start();

                content.destroy();

            }

            ctx.close();
        }
    }

    private static class ResponderThread extends Thread
    {
        private final ZFrame address;
        private final Socket responderSocket;

        public ResponderThread(final ZFrame address, final Socket responderSocket)
        {

            this.address = address;
            this.responderSocket = responderSocket;
        }

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(10000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            //  Send reply
            address.send(responderSocket, ZFrame.REUSE + ZFrame.MORE);

            ZFrame response = new ZFrame("Farlopero");
            response.send(responderSocket, ZFrame.REUSE);
            address.destroy();
            response.destroy();
        }
    }

    //The main thread simply starts several clients, and a server, and then
    //waits for the server to finish.

    public static void main(String[] args) throws Exception
    {
       Client client1 = new Client(UUID.randomUUID());
        client1.start();

        Client client2 = new Client(UUID.randomUUID());
        client2.start();

        //new Thread(new Client()).start();
        //new Thread(new Client()).start();
        new Thread(new Server()).start();

        //  Run for 5 seconds then quit
        while(true)
        {
            Thread.sleep(5 * 1000);
        }

        //zmqContext.destroy();
    }
}
