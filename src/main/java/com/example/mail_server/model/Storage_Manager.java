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
    private final List <User> valid_users; // TODO modify this to be dynamic
    private final ObjectMapper mapper;

    public Storage_Manager ()
    {
        new File (DATA_DIR).mkdirs ();
        mapper = new ObjectMapper ();
        mapper.registerModule (new JavaTimeModule ());

        user_locks = new ConcurrentHashMap <> ();
        valid_users = List.of
                (
                        new User ("mail1", "1"),
                        new User ("mail2", "2"),
                        new User ("mail3", "2")
                );
    }

    private Object get_Lock (String user_email)
    {
        return user_locks.computeIfAbsent (user_email, _ -> new Object ());
    }

    public List <Email> load_Inbox (String user_email)
    {
        synchronized (get_Lock (user_email))
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

    public void save_Inbox (String user_email, List <Email> emails)
    {
        synchronized (get_Lock (user_email))
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

    public void deliver_Email (String user_email, Email new_email)
    {
        synchronized (get_Lock (user_email))
        {
            List <Email> inbox = load_Inbox (user_email);
            inbox.add (new_email);
            save_Inbox (user_email, inbox);
        }
    }

    /**
     * Removes a specific email from a user's inbox and saves the updated list.
     */
    public boolean delete_Email (String username, Email email_to_delete)
    {
        List <Email> inbox = load_Inbox (username);

        // Remove the email that has the exact same ID
        boolean removed = inbox.removeIf (email -> email.id ().equals (email_to_delete.id ()));

        if (removed)
        {
            // If we successfully found and removed it, save the new list back to the JSON file
            save_Inbox (username, inbox);
        }

        return removed;
    }

    public List <User> get_Users ()
    {
        return valid_users;
    }
}
