package com.example.mail_server;

import com.example.mail_server.model.Storage_Manager;
import com.example.shared.data.Email;
import com.example.shared.data.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class StorageManagerTest
{

    private final String user1 = "giorgio@mia.mail.com";
    private final String user2 = "mario@mia.mail.com";
    private Storage_Manager storageManager;

    @BeforeEach
    void setUp ()
    {
        storageManager = new Storage_Manager ();
        new File ("server_data/" + user1 + ".json").delete ();
        new File ("server_data/" + user2 + ".json").delete ();
    }

    @AfterEach
    void tearDown ()
    {
        new File ("server_data/" + user1 + ".json").delete ();
        new File ("server_data/" + user2 + ".json").delete ();
    }

    @Test
    void testLoadNonExistentUser ()
    {
        List <Email> inbox = storageManager.load_Inbox ("ghost@mia.mail.com");
        assertNotNull (inbox);
        assertTrue (inbox.isEmpty ());
    }

    @Test
    void testDeliverAndLoadSingleEmail ()
    {
        User sender = new User (user2, "pass");
        User receiver = new User (user1, "pass");
        Email email = new Email ("1", sender, List.of (receiver), "S1", "T1", LocalDateTime.now ());

        storageManager.deliver_Email (user1, email);
        List <Email> inbox = storageManager.load_Inbox (user1);

        assertEquals (1, inbox.size ());
        assertEquals ("1", inbox.getFirst ().id ());
        assertEquals ("S1", inbox.getFirst ().subject ());
    }

    @Test
    void testDeliverMultipleEmails ()
    {
        User sender = new User (user2, "pass");
        User receiver = new User (user1, "pass");
        Email e1 = new Email ("1", sender, List.of (receiver), "S1", "T1", LocalDateTime.now ());
        Email e2 = new Email ("2", sender, List.of (receiver), "S2", "T2", LocalDateTime.now ());

        storageManager.deliver_Email (user1, e1);
        storageManager.deliver_Email (user1, e2);
        List <Email> inbox = storageManager.load_Inbox (user1);

        assertEquals (2, inbox.size ());
        assertEquals ("1", inbox.get (0).id ());
        assertEquals ("2", inbox.get (1).id ());
    }

    @Test
    void testConcurrentDeliveryMutualExclusion () throws InterruptedException
    {
        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool (50);
        CountDownLatch latch = new CountDownLatch (threads);
        User sender = new User (user2, "pass");
        User receiver = new User (user1, "pass");

        for (int i = 0; i < threads; i++)
        {
            executor.submit (() -> {
                Email email = new Email (UUID.randomUUID ().toString (), sender, List.of (receiver), "Subj", "Text",
                                         LocalDateTime.now ());
                storageManager.deliver_Email (user1, email);
                latch.countDown ();
            });
        }

        latch.await ();
        executor.shutdown ();
        List <Email> inbox = storageManager.load_Inbox (user1);
        assertEquals (threads, inbox.size ());
    }

    @Test
    void testGetUsersIsPopulated ()
    {
        List <User> users = storageManager.get_Users ();
        assertNotNull (users);
        assertFalse (users.isEmpty ());
        assertTrue (users.stream ().anyMatch (u -> u.username ().equals ("giorgio@mia.mail.com")));
    }
}