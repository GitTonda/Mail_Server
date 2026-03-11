package com.example.mail_server.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client_Handler implements Runnable
{
    private final Socket socket;
    private final Server_Model model;

    public Client_Handler (Socket socket, Server_Model model)
    {
        this.socket = socket;
        this.model = model;
    }

    @Override
    public void run ()
    {
        try (
                BufferedReader in = new BufferedReader (new InputStreamReader (socket.getInputStream ()));
                PrintWriter out = new PrintWriter (socket.getOutputStream (), true)
        )
        {
            while (true)
            {
                String clientMessage = in.readLine ();
                if (clientMessage != null)
                {
                    model.append_Log ("Received from client: " + clientMessage);
                    out.println ("SERVER ECHO: " + clientMessage);
                }
            }
        }
        catch (IOException e)
        {
            System.err.println ("Communication error with client: " + e.getMessage ());
        }
    }
}
