package com.example.mail_server.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Server_Model
{
    private final StringProperty latest_Log;
    private Storage_Manager storage_manager;
    private Thread server_thread;

    public Server_Model ()
    {
        latest_Log = new SimpleStringProperty ("");
        storage_manager = new Storage_Manager ();
    }

    public void append_Log (String message)
    {
        String timestamp = LocalTime.now ().format (DateTimeFormatter.ofPattern ("[HH:mm:ss.SSS]"));
        System.out.println (timestamp + ": " + message);
        // Platform.runLater (() -> latest_Log.set (timestamp + " | " + message)); // TODO uncomment this for logs
    }

    public StringProperty latest_Log_Property ()
    {
        return latest_Log;
    }

    public void start_Server ()
    {
        server_thread = Thread.startVirtualThread (new Server_Handler (this));
    }

    public void stop_Server ()
    {
        if (server_thread != null)
        {
            server_thread.interrupt ();
            append_Log ("Server stopped.");
        }
    }

    public Storage_Manager get_Storage_manager ()
    {
        return storage_manager;
    }
}