package com.example.mail_server.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Server_Model
{
    private final Storage_Manager storage_manager;
    private final StringProperty log;
    private Thread server_thread;

    public Server_Model ()
    {
        storage_manager = new Storage_Manager ();
        log = new SimpleStringProperty ("");
    }

    public void append_Log (String message)
    {
        String timestamp = LocalTime.now ().format (DateTimeFormatter.ofPattern ("[HH:mm:ss.SSS]"));
        System.out.println (timestamp + ": " + message);
        // Platform.runLater (() -> latest_Log.set (timestamp + " | " + message)); // TODO uncomment this for logs
    }

    public StringProperty get_Log ()
    {
        return log;
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

    public Storage_Manager get_Storage_Manager ()
    {
        return storage_manager;
    }
}