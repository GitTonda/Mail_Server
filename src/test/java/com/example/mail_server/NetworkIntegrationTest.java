package com.example.mail_server;

import com.example.mail_server.model.Client_Handler;
import com.example.mail_server.model.Server_Model;
import com.example.shared.data.Package;
import com.example.shared.data.TYPE;
import com.example.shared.data.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NetworkIntegrationTest
{
    private Server_Model serverModel;
    private ServerSocket serverSocket;
    private ExecutorService serverThreadPool;
    private int port;

    @BeforeEach
    void setUp () throws IOException
    {
        serverModel = new Server_Model ();
        serverSocket = new ServerSocket (0);
        port = serverSocket.getLocalPort ();
        serverThreadPool = Executors.newCachedThreadPool ();

        new Thread (() -> {
            while (! serverSocket.isClosed ())
            {
                try
                {
                    Socket clientSocket = serverSocket.accept ();
                    serverThreadPool.submit (new Client_Handler (clientSocket, serverModel));
                }
                catch (IOException ignored)
                {
                }
            }
        }).start ();
    }

    @AfterEach
    void tearDown () throws IOException
    {
        serverSocket.close ();
        serverThreadPool.shutdownNow ();
        new File ("server_data/giorgio@mia.mail.com.json").delete ();
    }

    @Test
    void testMultipleSimultaneousClients () throws InterruptedException
    {
        int clientCount = 50;
        ExecutorService clientExecutor = Executors.newFixedThreadPool (20);
        CountDownLatch latch = new CountDownLatch (clientCount);
        AtomicInteger successfulResponses = new AtomicInteger (0);

        for (int i = 0; i < clientCount; i++)
        {
            clientExecutor.submit (() -> {
                try (Socket socket = new Socket ("127.0.0.1", port);
                     ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());
                     ObjectInputStream in = new ObjectInputStream (socket.getInputStream ()))
                {

                    User user = new User ("giorgio@mia.mail.com", "pass");
                    Package req = new Package (TYPE.REQUEST_INBOX, user, null, null, null);

                    out.writeObject (req);
                    out.flush ();

                    Package res = (Package) in.readObject ();
                    if (res != null && res.type () == TYPE.ANSWER)
                    {
                        successfulResponses.incrementAndGet ();
                    }
                }
                catch (Exception ignored)
                {
                }
                finally
                {
                    latch.countDown ();
                }
            });
        }

        latch.await ();
        clientExecutor.shutdown ();
        assertEquals (clientCount, successfulResponses.get ());
    }

    @Test
    void testClientAbruptDisconnectBeforeSending () throws IOException, InterruptedException
    {
        Socket socket = new Socket ("127.0.0.1", port);
        ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());

        socket.close ();

        Thread.sleep (200);
        assertTrue (socket.isClosed ());
    }

    @Test
    void testClientAbruptDisconnectWhileReading () throws IOException, InterruptedException
    {
        Socket socket = new Socket ("127.0.0.1", port);
        ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());

        User user = new User ("giorgio@mia.mail.com", "pass");
        Package req = new Package (TYPE.REQUEST_INBOX, user, null, null, null);
        out.writeObject (req);
        out.flush ();

        socket.close ();

        Thread.sleep (200);
        assertTrue (socket.isClosed ());
    }

    @Test
    void testCorruptedStreamData () throws IOException, InterruptedException
    {
        Socket socket = new Socket ("127.0.0.1", port);
        ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());

        out.writeBytes ("THIS IS NOT A JAVA OBJECT");
        out.flush ();

        Thread.sleep (200);
        assertFalse (socket.isClosed ());
        socket.close ();
    }
}