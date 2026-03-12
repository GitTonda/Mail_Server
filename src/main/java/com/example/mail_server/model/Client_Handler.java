package com.example.mail_server.model;

import com.example.shared.data.Email;
import com.example.shared.data.Package;
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
            case SEND_EMAIL -> send_email (pkg);
            case REQUEST_INBOX -> request_inbox (pkg);
            case ANSWER -> new Package (ANSWER, null, null, null, "ERR: Server does not accept ANSWER packages.");
        };
    }

    private Package check_credentials (Package pkg)
    {
        return new Package (LOGIN, null, null, null,
                            storage.get_Users ().contains (pkg.user ()) ? "SUCCESS" : "FAILURE");
    }

    private Package send_email (Package pkg)
    {
        Email email = pkg.email ();
        String sender = email.sender ().username ();
        List <String> receivers = email.receivers ().stream ().map (User :: username).toList ();
        List <String> valid_users = storage.get_Users ().stream ().map (User :: username).toList ();
        StringBuilder error_msg = new StringBuilder ();

        for (String r : receivers)
        {
            if (valid_users.contains (r))
            {
                storage.deliver_Email (r, email);
                model.append_Log ("[" + sender + "] --> [" + r + "] : email sent");
            }
            else
            {
                error_msg.append ("[").append (r).append ("] DOES NOT EXIST\n");
                model.append_Log ("[" + sender + "] -/> [" + r + "] : not a valid address");
            }
        }

        return new Package (SEND_EMAIL, null, null, null,
                            error_msg.isEmpty () ? error_msg.toString () : "SUCCESS");
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

        model.append_Log ("Sending " + new_emails.size () + " new emails to " + username);
        return new Package (REQUEST_INBOX, null, null, new_emails, null);
    }

    @Override
    public void run () // TODO reformat log output
    {
        try (
                ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());
                ObjectInputStream in = new ObjectInputStream (socket.getInputStream ())
        )
        {
            Object in_obj = in.readObject ();
            if (in_obj instanceof Package pkg)
            {
                model.append_Log ("Received package of type [" + pkg.type () + "] from: " + pkg.user ());
                out.writeObject (handle_request (pkg));
                model.append_Log ("Sending " + pkg.type () + " to " + pkg.user ());
                out.flush ();
            }
            else model.append_Log ("Received unknown object format from client.");
        }
        catch (IOException e)
        {
            model.append_Log ("Communication error with client: " + e.getMessage ());
        }
        catch (ClassNotFoundException e)
        {
            model.append_Log ("Failed to deserialize object: " + e.getMessage ());
        }
    }
}