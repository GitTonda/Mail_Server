package com.example.mail_server.model;

import com.example.shared.data.Email;
import com.example.shared.data.Package;
import com.example.shared.data.TYPE;
import com.example.shared.data.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.example.shared.data.TYPE.*;

public class Client_Handler implements Runnable
{
    private final Socket socket;
    private final Server_Model model;
    private final Storage_Manager storage;

    public Client_Handler (Socket socket, Server_Model model)
    {
        this.socket = socket;
        this.model = model;
        storage = model.get_Storage_Manager ();
    }

    private Package handle_request (Package pkg) throws IOException
    {
        return switch (pkg.type ())
        {
            case LOGIN -> check_credentials (pkg);
            case REQUEST_INBOX -> request_inbox (pkg);
            case DELETE_EMAIL -> delete_email (pkg);
            case SEND_EMAIL, ANSWER, FORWARD -> process_outgoing_email (pkg);
        };
    }

    private Package check_credentials (Package pkg)
    {
        return new Package (LOGIN, null, null, null,
                            storage.get_Users ().contains (pkg.user ()) ? "SUCCESS" : "FAILURE");
    }

    private Package process_outgoing_email (Package pkg)
    {
        Email email_to_deliver = pkg.email ();

        if (pkg.type () == ANSWER || pkg.type () == FORWARD)
        {
            if (pkg.email_list () != null && ! pkg.email_list ().isEmpty ())
            {
                Email original = pkg.email_list ().get (0);
                String history = build_tamper_proof_history (pkg.type (), original);

                email_to_deliver = new Email (
                        email_to_deliver.id (),
                        email_to_deliver.sender (),
                        email_to_deliver.receivers (),
                        email_to_deliver.subject (),
                        email_to_deliver.text () + history,
                        email_to_deliver.date ()
                );
            }
        }

        String sender = email_to_deliver.sender ().username ();
        List <String> receivers = email_to_deliver.receivers ().stream ().map (User :: username).toList ();
        List <String> valid_users = storage.get_Users ().stream ().map (User :: username).toList ();
        StringBuilder error_msg = new StringBuilder ();

        for (String r : receivers)
        {
            if (valid_users.contains (r))
            {
                storage.deliver_Email (r, email_to_deliver);
                model.append_log (
                        String.format ("[ACTION] %-12s %s: Delivered to [%s]", "[" + sender + "]", pkg.type (), r));
            }
            else
            {
                error_msg.append ("[").append (r).append ("] DOES NOT EXIST\n");
                model.append_log (
                        String.format ("[ERROR]  %-12s %s: User [%s] DOES NOT EXIST", "[" + sender + "]", pkg.type (),
                                       r));
            }
        }

        model.increment_emails ();
        return new Package (pkg.type (), null, null, null, error_msg.isEmpty () ? "SUCCESS" : error_msg.toString ());
    }

    // Helper method to format the quote block perfectly on the server
    private String build_tamper_proof_history (TYPE type, Email original)
    {
        String date_str = original.date ().toString ().replace ("T", " ").substring (0, 16);

        // This regex trick adds a "> " to the beginning of EVERY line in the original text
        // The "(?m)^" means "match the start of every new line"
        String quoted_body = original.text ().replaceAll ("(?m)^", "> ");

        if (type == ANSWER)
        {
            return "\n\nOn " + date_str + ", " + original.sender ().username () + " wrote:\n"
                    + quoted_body;
        }
        else
        { // FORWARD
            return "\n\n---------- Forwarded message ---------\n"
                    + "From: " + original.sender ().username () + "\n"
                    + "Date: " + date_str + "\n"
                    + "Subject: " + original.subject () + "\n\n"
                    + quoted_body;
        }
    }

    private Package request_inbox (Package pkg)
    {
        String last_id = pkg.message ();
        String username = pkg.user ().username ();
        List <Email> all_emails = storage.load_Inbox (username);
        List <Email> new_emails = new ArrayList <> ();

        if (last_id == null || last_id.isEmpty ()) new_emails = all_emails;
        else
        {
            boolean found = false;
            for (Email m : all_emails)
            {
                if (found) new_emails.add (m);
                else if (m.id ().equals (last_id)) found = true;
            }
            if (! found) new_emails = all_emails;
        }

        model.append_log (String.format ("[REQ]    %-12s REQUEST_INBOX: %d new emails sent", "[" + username + "]",
                                         new_emails.size ()));
        return new Package (REQUEST_INBOX, null, null, new_emails, null);
    }

    private Package delete_email (Package pkg)
    {
        if (pkg.user () == null || pkg.email () == null)
        {
            model.append_log ("Failed to delete email: Missing user or email data in package.");
            return new Package (DELETE_EMAIL, null, null, null, "FAILURE: Invalid request");
        }

        String username = pkg.user ().username ();
        Email email_to_delete = pkg.email ();

        // Tell the storage manager to remove it from the user's inbox file
        boolean success = storage.delete_Email (username, email_to_delete);

        if (success)
        {
            model.append_log ("[" + username + "] deleted email: " + email_to_delete.subject ());
            return new Package (DELETE_EMAIL, null, null, null, "SUCCESS");
        }
        else
        {
            model.append_log ("[" + username + "] failed to delete email (not found): " + email_to_delete.subject ());
            return new Package (DELETE_EMAIL, null, null, null, "FAILURE: Email not found");
        }
    }

    @Override
    public void run () // TODO reformat log output
    {
        model.increment_connections ();
        try (
                ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());
                ObjectInputStream in = new ObjectInputStream (socket.getInputStream ())
        )
        {
            Object in_obj = in.readObject ();
            if (in_obj instanceof Package pkg)
            {
                String username = (pkg.user () != null) ? "[" + pkg.user ().username () + "]" : "[UNKNOWN]";
                model.append_log (String.format ("[INFO]   %-12s Processing package: %s", username, pkg.type ()));
                out.writeObject (handle_request (pkg));
                out.flush ();
            }
            else model.append_log ("Received unknown object format from client.");
        }
        catch (IOException e)
        {
            model.append_log ("Communication error with client: " + e.getMessage ());
        }
        catch (ClassNotFoundException e)
        {
            model.append_log ("Failed to deserialize object: " + e.getMessage ());
        }
    }
}