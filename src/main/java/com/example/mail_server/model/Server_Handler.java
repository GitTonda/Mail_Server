package com.example.mail_server.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server_Handler implements Runnable
{
    private static final int PORT = 8181;
    private final ExecutorService thread_pool;
    private final Server_Model model;
    private volatile boolean is_running;

    public Server_Handler (Server_Model server_model)
    {
        is_running = false;
        model = server_model;
        thread_pool = Executors.newCachedThreadPool ();
    }

    @Override
    public void run ()
    {
        if (is_running) return;
        is_running = true;

        model.append_Log ("[Server Started]");
        try (ServerSocket serverSocket = new ServerSocket (PORT))
        {
            while (is_running)
            {
                Socket clientSocket = serverSocket.accept ();
                model.append_Log ("[Client Connected] : " + clientSocket.getInetAddress ());
                thread_pool.execute (new Client_Handler (clientSocket, model));
            }
        }
        catch (IOException e)
        {
            if (is_running) System.err.println ("[Server Exception] : " + e.getMessage ());
            else System.out.println ("[Server Shutdown]");
        }
    }
}
