package com.example.mail_server.model;

import com.example.shared.data.*;
import com.example.shared.data.Package;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
        storage = model.get_storage_manager ();
    }

    private Package handle_request (Package pkg)
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
                            storage.get_users ().contains (pkg.user ()) ? "SUCCESS" : "FAILURE");
    }

    private Package process_outgoing_email (Package pkg)
    {
        Email email_to_deliver = pkg.email ();

        if (pkg.type () == ANSWER || pkg.type () == FORWARD)
        {
            if (pkg.email_list () != null && ! pkg.email_list ().isEmpty ())
            {
                Email original = pkg.email_list ().get (0);
                String history = build_history (pkg.type (), original);

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
        List <String> valid_users = storage.get_users ().stream ().map (User :: username).toList ();
        StringBuilder error_msg = new StringBuilder ();

        for (String r : receivers)
        {
            if (valid_users.contains (r))
            {
                storage.deliver_email (r, email_to_deliver);
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

    private String build_history (TYPE type, Email original)
    {
        String date_str = original.date ().toString ().replace ("T", " ").substring (0, 16);
        String quoted_body = original.text ().replaceAll ("(?m)^", "> ");

        if (type == ANSWER)
            return "\n\nOn " + date_str + ", " + original.sender ().username () + " wrote:\n" + quoted_body;

        else
            return "\n\n---------- Forwarded message ---------\n"
                    + "From: " + original.sender ().username () + "\n"
                    + "Date: " + date_str + "\n"
                    + "Subject: " + original.subject () + "\n\n"
                    + quoted_body;
    }

    private Package request_inbox (Package pkg)
    {
        String last_id = pkg.message ();
        String username = pkg.user ().username ();
        List <Email> all_emails = storage.load_inbox (username);
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
            model.append_log (String.format ("[ERROR]  %-12s DELETE_EMAIL: Missing user or email data", "[UNKNOWN]"));
            return new Package (DELETE_EMAIL, null, null, null, "FAILURE: Invalid request");
        }

        String username = pkg.user ().username ();
        Email email_to_delete = pkg.email ();

        boolean success = storage.delete_email (username, email_to_delete);
        if (success)
        {
            model.append_log (String.format ("[ACTION] %-12s DELETE_EMAIL: %s", "[" + username + "]",
                                             email_to_delete.subject ()));
            return new Package (DELETE_EMAIL, null, null, null, "SUCCESS");
        }
        else
        {
            model.append_log (String.format ("[ERROR]  %-12s DELETE_EMAIL: Not found - %s", "[" + username + "]",
                                             email_to_delete.subject ()));
            return new Package (DELETE_EMAIL, null, null, null, "FAILURE: Email not found");
        }
    }

    @Override
    public void run ()
    {
        model.increment_connections ();
        try (
                BufferedReader in = new BufferedReader (new InputStreamReader (socket.getInputStream ()));
                PrintWriter out = new PrintWriter (socket.getOutputStream (), true)
        )
        {
            String json_request = in.readLine ();
            if (json_request != null)
            {
                Package pkg = Json_Mapper.get ().readValue (json_request, Package.class);
                String username = (pkg.user () != null) ? "[" + pkg.user ().username () + "]" : "[UNKNOWN]";
                model.append_log (String.format ("[INFO]   %-12s Processing package: %s", username, pkg.type ()));
                Package response_pkg = handle_request (pkg);
                String json_response = Json_Mapper.get ().writeValueAsString (response_pkg);
                out.println (json_response);
            }
            else model.append_log (String.format ("[INFO]   %-12s A Client reconnected", "[SERVER]"));
        }
        catch (Exception e)
        {
            model.append_log (String.format ("[ERROR]  %-12s Communication error: %s", "[SERVER]", e.getMessage ()));
        }
    }
}