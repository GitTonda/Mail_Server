package com.example.mail_server.model;

import com.example.shared.data.Email;
import com.example.shared.data.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Storage_Manager
{
    private static final String DATA_DIR = "server_data/";
    private final ConcurrentHashMap <String, Object> user_Locks;
    private final ObjectMapper mapper;
    private final List <User> validUsers; // TODO modify this to be dynamic

    public Storage_Manager ()
    {
        this.user_Locks = new ConcurrentHashMap <> ();
        this.mapper = new ObjectMapper ();
        this.mapper.registerModule (new JavaTimeModule ());
        new File (DATA_DIR).mkdirs ();

        this.validUsers = List.of (
                new User ("giorgio@mia.mail.com", "giorgio"),
                new User ("mario@mia.mail.com", "mario"),
                new User ("luigi@mia.mail.com", "luigi")
        );
    }

    private Object get_Lock (String userEmail)
    {
        return user_Locks.computeIfAbsent (userEmail, _ -> new Object ());
    }

    public List <Email> load_Inbox (String userEmail)
    {
        synchronized (get_Lock (userEmail))
        {
            File file = new File (DATA_DIR + userEmail + ".json");
            if (! file.exists ()) return new ArrayList <> ();

            try
            {
                return mapper.readValue (file, new TypeReference <> () {});
            }
            catch (IOException e)
            {
                System.err.println ("Failed to read inbox for " + userEmail + ": " + e.getMessage ());
                return new ArrayList <> ();
            }
        }
    }

    public void save_Inbox (String userEmail, List <Email> emails)
    {
        synchronized (get_Lock (userEmail))
        {
            File file = new File (DATA_DIR + userEmail + ".json");
            try
            {
                mapper.writerWithDefaultPrettyPrinter ().writeValue (file, emails);
            }
            catch (IOException e)
            {
                System.err.println ("Failed to save inbox for " + userEmail + ": " + e.getMessage ());
            }
        }
    }

    public void deliver_Email (String userEmail, Email newEmail)
    {
        synchronized (get_Lock (userEmail))
        {
            List <Email> inbox = load_Inbox (userEmail);
            inbox.add (newEmail);
            save_Inbox (userEmail, inbox);
        }
    }

    public List <User> get_Users ()
    {
        return validUsers;
    }
}
