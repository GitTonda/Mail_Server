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
    private final List <User> valid_users;
    private final ObjectMapper mapper;

    public Storage_Manager ()
    {
        new File (DATA_DIR).mkdirs ();
        mapper = new ObjectMapper ();
        mapper.registerModule (new JavaTimeModule ());

        user_locks = new ConcurrentHashMap <> ();
        valid_users = List.of
                (
                        new User ("mail1@domain", "1"),
                        new User ("mail2@domain", "2"),
                        new User ("mail3@domain", "3"),
                        new User ("mail4@domain", "4")
                );
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
