package com.example.mail_server;

import com.example.mail_server.model.Client_Handler;
import com.example.mail_server.model.Server_Model;
import com.example.shared.data.Email;
import com.example.shared.data.Package;
import com.example.shared.data.TYPE;
import com.example.shared.data.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientHandlerTest
{

    private final String validUser1 = "giorgio@mia.mail.com";
    private final String validUser2 = "mario@mia.mail.com";
    private final String invalidUser = "fake@mia.mail.com";
    private Server_Model serverModel;

    @BeforeEach
    void setUp ()
    {
        serverModel = new Server_Model ();
        new File ("server_data/" + validUser1 + ".json").delete ();
        new File ("server_data/" + validUser2 + ".json").delete ();
    }

    @AfterEach
    void tearDown ()
    {
        new File ("server_data/" + validUser1 + ".json").delete ();
        new File ("server_data/" + validUser2 + ".json").delete ();
    }

    private Package sendRequestAndGetResponse (Object requestPayload) throws Exception
    {
        try (ServerSocket serverSocket = new ServerSocket (0))
        {
            int port = serverSocket.getLocalPort ();

            Thread serverThread = new Thread (() -> {
                try
                {
                    Socket clientSocket = serverSocket.accept ();
                    Client_Handler handler = new Client_Handler (clientSocket, serverModel);
                    handler.run ();
                }
                catch (Exception ignored)
                {
                }
            });
            serverThread.start ();

            try (Socket socket = new Socket ("127.0.0.1", port);
                 ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());
                 ObjectInputStream in = new ObjectInputStream (socket.getInputStream ()))
            {

                out.writeObject (requestPayload);
                out.flush ();
                return (Package) in.readObject ();
            }
        }
    }

    @Test
    void testRequestInboxInvalidUser () throws Exception
    {
        User user = new User (invalidUser, "pass");
        Package req = new Package (TYPE.REQUEST_INBOX, user, null, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res);
        assertEquals (TYPE.ANSWER, res.type ());
        assertTrue (res.message ().contains ("ERR"));
        assertNull (res.email_list ());
    }

    @Test
    void testRequestInboxEmpty () throws Exception
    {
        User user = new User (validUser1, "pass");
        Package req = new Package (TYPE.REQUEST_INBOX, user, null, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res);
        assertEquals (TYPE.ANSWER, res.type ());
        assertEquals ("SUCCESS", res.message ());
        assertNotNull (res.email_list ());
        assertTrue (res.email_list ().isEmpty ());
    }

    @Test
    void testRequestInboxAllMessages () throws Exception
    {
        User u1 = new User (validUser1, "pass");
        User u2 = new User (validUser2, "pass");
        Email e1 = new Email ("1", u2, List.of (u1), "S1", "T1", LocalDateTime.now ());
        serverModel.get_Storage_Manager ().deliver_Email (validUser1, e1);

        Package req = new Package (TYPE.REQUEST_INBOX, u1, null, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res.email_list ());
        assertEquals (1, res.email_list ().size ());
        assertEquals ("1", res.email_list ().getFirst ().id ());
    }

    @Test
    void testRequestInboxIncrementalValidId () throws Exception
    {
        User u1 = new User (validUser1, "pass");
        User u2 = new User (validUser2, "pass");
        Email e1 = new Email ("id_1", u2, List.of (u1), "S1", "T1", LocalDateTime.now ());
        Email e2 = new Email ("id_2", u2, List.of (u1), "S2", "T2", LocalDateTime.now ());
        Email e3 = new Email ("id_3", u2, List.of (u1), "S3", "T3", LocalDateTime.now ());
        serverModel.get_Storage_Manager ().deliver_Email (validUser1, e1);
        serverModel.get_Storage_Manager ().deliver_Email (validUser1, e2);
        serverModel.get_Storage_Manager ().deliver_Email (validUser1, e3);

        Package req = new Package (TYPE.REQUEST_INBOX, u1, null, null, "id_2");
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res.email_list ());
        assertEquals (1, res.email_list ().size ());
        assertEquals ("id_3", res.email_list ().getFirst ().id ());
    }

    @Test
    void testRequestInboxIncrementalInvalidIdFallback () throws Exception
    {
        User u1 = new User (validUser1, "pass");
        User u2 = new User (validUser2, "pass");
        Email e1 = new Email ("id_1", u2, List.of (u1), "S1", "T1", LocalDateTime.now ());
        serverModel.get_Storage_Manager ().deliver_Email (validUser1, e1);

        Package req = new Package (TYPE.REQUEST_INBOX, u1, null, null, "ghost_id");
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res.email_list ());
        assertEquals (1, res.email_list ().size ());
    }

    @Test
    void testSendEmailValidReceiver () throws Exception
    {
        User sender = new User (validUser2, "pass");
        User receiver = new User (validUser1, "pass");
        Email e1 = new Email ("1", sender, List.of (receiver), "S1", "T1", LocalDateTime.now ());

        Package req = new Package (TYPE.SEND_EMAIL, sender, e1, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res);
        assertEquals ("SUCCESS", res.message ());
        assertEquals (1, serverModel.get_Storage_Manager ().load_Inbox (validUser1).size ());
    }

    @Test
    void testSendEmailMixedReceivers () throws Exception
    {
        User sender = new User (validUser2, "pass");
        User validRec = new User (validUser1, "pass");
        User invalidRec = new User (invalidUser, "pass");
        Email e1 = new Email ("1", sender, List.of (validRec, invalidRec), "S1", "T1", LocalDateTime.now ());

        Package req = new Package (TYPE.SEND_EMAIL, sender, e1, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res);
        assertTrue (res.message ().contains ("ERROR"));
        assertTrue (res.message ().contains (invalidUser));
        assertEquals (1, serverModel.get_Storage_Manager ().load_Inbox (validUser1).size ());
    }

    @Test
    void testSendEmailAllInvalidReceivers () throws Exception
    {
        User sender = new User (validUser2, "pass");
        User invalidRec = new User (invalidUser, "pass");
        Email e1 = new Email ("1", sender, List.of (invalidRec), "S1", "T1", LocalDateTime.now ());

        Package req = new Package (TYPE.SEND_EMAIL, sender, e1, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res);
        assertTrue (res.message ().contains ("ERR"));
    }

    @Test
    void testSendAnswerTypeRequest () throws Exception
    {
        User sender = new User (validUser1, "pass");
        Package req = new Package (TYPE.ANSWER, sender, null, null, null);
        Package res = sendRequestAndGetResponse (req);

        assertNotNull (res);
        assertEquals (TYPE.ANSWER, res.type ());
        assertTrue (res.message ().contains ("ERR"));
    }

    @Test
    void testSendInvalidObject ()
    {
        String invalidPayload = "This is a raw string, not a Package object";

        assertThrows (java.io.EOFException.class, () -> {
            sendRequestAndGetResponse (invalidPayload);
        }, "Server should drop the connection and cause EOFException on invalid objects");
    }
}