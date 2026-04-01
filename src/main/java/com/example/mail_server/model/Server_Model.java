package com.example.mail_server.model;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Server_Model
{
    private final Storage_Manager storage_manager;
    private final StringProperty log;
    private final StringProperty server_info = new SimpleStringProperty ();
    private final LocalDateTime start_time = LocalDateTime.now ();
    private Thread server_thread;
    private int total_connections = 0;
    private int emails_processed = 0;

    public Server_Model ()
    {
        storage_manager = new Storage_Manager ();
        log = new SimpleStringProperty ("");
    }

    public void start_Server ()
    {
        server_thread = Thread.startVirtualThread (new Server_Handler (this));
        refresh_info_panel ();
    }

    public void stop_Server ()
    {
        if (server_thread != null)
        {
            server_thread.interrupt ();
            append_log ("Server stopped.");
        }
    }

    public void increment_connections ()
    {
        total_connections++;
        refresh_info_panel ();
    }

    public void increment_emails ()
    {
        emails_processed++;
        refresh_info_panel ();
    }

    public void refresh_info_panel ()
    {
        Duration uptime = Duration.between (start_time, LocalDateTime.now ());
        String formatted_uptime = String.format ("%02d:%02d:%02d", uptime.toHours (), uptime.toMinutesPart (),
                                                 uptime.toSecondsPart ());

        int user_count = (get_storage_manager () != null && get_storage_manager ().get_users () != null)
                ? get_storage_manager ().get_users ().size () : 0;

        String dashboard = "🚀 SYSTEM STATUS\n" +
                "--------------------\n" +
                "Uptime:       " + formatted_uptime + "\n" +
                "Connections:  " + total_connections + "\n" +
                "Emails Sent:  " + emails_processed + "\n" +
                "Total Users:  " + user_count + "\n\n" +
                "🌍 NETWORK\n" +
                "--------------------\n" +
                "Host: 127.0.0.1\n" +
                "Port: 8181";

        javafx.application.Platform.runLater (() -> server_info.set (dashboard));
    }

    public void append_log (String message)
    {
        String timestamp = LocalTime.now ().format (DateTimeFormatter.ofPattern ("[HH:mm:ss.SSS]"));
        Platform.runLater (() -> log.set (timestamp + " | " + message));
    }

    public StringProperty get_log ()
    {
        return log;
    }

    public Storage_Manager get_storage_manager ()
    {
        return storage_manager;
    }

    public StringProperty server_info_Property ()
    {
        return server_info;
    }
}