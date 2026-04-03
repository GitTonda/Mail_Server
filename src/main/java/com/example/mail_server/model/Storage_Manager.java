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
    private final ConcurrentHashMap <String, Object> user_locks;
    private final ObjectMapper mapper;
    private final File users_file;
    private List <User> valid_users;

    public Storage_Manager ()
    {
        new File (DATA_DIR).mkdirs ();
        mapper = new ObjectMapper ();
        mapper.registerModule (new JavaTimeModule ());

        user_locks = new ConcurrentHashMap <> ();
        users_file = new File (DATA_DIR + "users.json");

        if (users_file.exists ())
        {
            try
            {
                valid_users = mapper.readValue (users_file, new com.fasterxml.jackson.core.type.TypeReference <> () {});
            }
            catch (IOException e)
            {
                valid_users = new ArrayList <> ();
                System.err.println ("Failed to load users.json");
            }
        }
        else
        {
            valid_users = new ArrayList <> ();
            save_users ();
        }
    }

    private synchronized void save_users ()
    {
        try
        {
            mapper.writerWithDefaultPrettyPrinter ().writeValue (users_file, valid_users);
        }
        catch (IOException e)
        {
            System.err.println ("Failed to save users.json: " + e.getMessage ());
        }
    }

    public synchronized boolean register_user (User new_user)
    {
        boolean exists = valid_users.stream ().anyMatch (u -> u.username ().equals (new_user.username ()));
        if (exists) return false;

        save_inbox (new_user.username (), new ArrayList <> ());
        valid_users.add (new_user);
        save_users ();

        return true;
    }

    public List <Email> load_inbox (String user_email)
    {
        synchronized (get_lock (user_email))
        {
            File file = new File (DATA_DIR + user_email + ".json");
            if (! file.exists ()) return new ArrayList <> ();

            try
            {
                return mapper.readValue (file, new TypeReference <> () {});
            }
            catch (IOException e)
            {
                System.err.println ("Failed to read inbox for " + user_email + ": " + e.getMessage ());
                return new ArrayList <> ();
            }
        }
    }

    public void save_inbox (String user_email, List <Email> emails)
    {
        synchronized (get_lock (user_email))
        {
            File file = new File (DATA_DIR + user_email + ".json");
            try
            {
                mapper.writerWithDefaultPrettyPrinter ().writeValue (file, emails);
            }
            catch (IOException e)
            {
                System.err.println ("Failed to save inbox for " + user_email + ": " + e.getMessage ());
            }
        }
    }

    public void deliver_email (String user_email, Email new_email)
    {
        synchronized (get_lock (user_email))
        {
            List <Email> inbox = load_inbox (user_email);
            inbox.add (new_email);
            save_inbox (user_email, inbox);
        }
    }

    public boolean delete_email (String username, Email email_to_delete)
    {
        synchronized (get_lock (username))
        {
            List <Email> inbox = load_inbox (username);
            boolean removed = inbox.removeIf (email -> email.id ().equals (email_to_delete.id ()));
            if (removed) save_inbox (username, inbox);
            return removed;
        }
    }

    private Object get_lock (String user_email)
    {
        return user_locks.computeIfAbsent (user_email, _ -> new Object ());
    }

    public List <User> get_users ()
    {
        return valid_users;
    }
}
