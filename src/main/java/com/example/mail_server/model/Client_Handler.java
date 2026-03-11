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

public class Client_Handler implements Runnable
{
    private final Socket socket;
    private final Server_Model model;

    public Client_Handler (Socket socket, Server_Model model)
    {
        this.socket = socket;
        this.model = model;
    }


    private Package send_email (Package pkg)
    {
        Email email = pkg.email ();
        Storage_Manager sm = model.get_Storage_manager ();
        List <String> users = sm.get_Users ().stream ().map (User :: username).toList ();

        boolean errors = false;
        StringBuilder error_msg = new StringBuilder ();

        for (User receiver : email.receivers ())
        {
            if (users.contains (receiver.username ()))
            {
                sm.deliver_Email (receiver.username (), email);
                model.append_Log (
                        "Delivered email from " + email.sender ().username () + " to " + receiver.username ());
            }
            else
            {
                errors = true;
                error_msg.append ("ERR: ").append (receiver.username ()).append (" NOT_EXISTS\n");
                model.append_Log ("Failed delivery: " + receiver.username () + " is not a valid account.");
            }
        }

        return new Package (TYPE.ANSWER, pkg.user (), null, null,
                            errors ? error_msg.toString () : "SUCCESS");
    }

    private Package request_inbox (Package pkg)
    {
        Storage_Manager sm = model.get_Storage_manager ();
        List <String> users = sm.get_Users ().stream ().map (User :: username).toList ();

        if (! users.contains (pkg.user ().username ()))
        {
            model.append_Log ("No user found for " + pkg.user ().username ());
            return new Package (TYPE.ANSWER, pkg.user (), null, null, "ERR");
        }

        List <Email> allEmails = sm.load_Inbox (pkg.user ().username ());
        List <Email> newEmails = new ArrayList <> ();
        String lastKnownId = pkg.message ();

        if (lastKnownId == null || lastKnownId.isEmpty ()) newEmails = allEmails;
        else
        {
            boolean found = false;
            for (Email email : allEmails)
            {
                if (found) newEmails.add (email);
                else if (email.id ().equals (lastKnownId)) found = true;
            }
            if (! found) newEmails = allEmails;
        }

        model.append_Log ("Sending " + newEmails.size () + " new emails to " + pkg.user ().username ());
        return new Package (TYPE.ANSWER, pkg.user (), null, newEmails, "SUCCESS");
    }

    private Package handle_request (Package pkg) throws IOException
    {
        return switch (pkg.type ())
        {
            case SEND_EMAIL -> send_email (pkg);
            case REQUEST_INBOX -> request_inbox (pkg);
            case ANSWER ->
                    new Package (TYPE.ANSWER, pkg.user (), null, null, "ERR: Server does not accept ANSWER packages.");
        };
    }

    @Override
    public void run ()
    {
        try (
                ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());
                ObjectInputStream in = new ObjectInputStream (socket.getInputStream ())
        )
        {
            Object receivedObj = in.readObject ();
            if (receivedObj instanceof Package pkg)
            {
                model.append_Log ("Received package of type [" + pkg.type () + "] from: " + pkg.user ());
                out.writeObject (handle_request (pkg));
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