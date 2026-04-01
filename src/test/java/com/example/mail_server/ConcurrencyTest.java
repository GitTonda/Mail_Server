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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrencyTest
{

    private final String targetUser = "giorgio@mia.mail.com";
    private final String secondUser = "luigi@mia.mail.com";
    private Storage_Manager storageManager;

    @BeforeEach
    void setUp ()
    {
        storageManager = new Storage_Manager ();
        new File ("server_data/" + targetUser + ".json").delete ();
        new File ("server_data/" + secondUser + ".json").delete ();
    }

    @AfterEach
    void tearDown ()
    {
        new File ("server_data/" + targetUser + ".json").delete ();
        new File ("server_data/" + secondUser + ".json").delete ();
    }

    @Test
    void testConcurrentReads () throws InterruptedException
    {
        int threads = 200;
        ExecutorService executor = Executors.newFixedThreadPool (50);
        CountDownLatch latch = new CountDownLatch (threads);
        AtomicInteger successfulReads = new AtomicInteger (0);

        User sender = new User ("mario@mia.mail.com", "pass");
        User receiver = new User (targetUser, "pass");
        Email initialEmail = new Email ("1", sender, List.of (receiver), "S", "T", LocalDateTime.now ());
        storageManager.deliver_email (targetUser, initialEmail);

        for (int i = 0; i < threads; i++)
        {
            executor.submit (() -> {
                List <Email> inbox = storageManager.load_inbox (targetUser);
                if (inbox != null && inbox.size () == 1)
                {
                    successfulReads.incrementAndGet ();
                }
                latch.countDown ();
            });
        }

        latch.await ();
        executor.shutdown ();
        assertEquals (threads, successfulReads.get ());
    }

    @Test
    void testConcurrentReadsAndWrites () throws InterruptedException
    {
        int writeThreads = 50;
        int readThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool (20);
        CountDownLatch latch = new CountDownLatch (writeThreads + readThreads);

        User sender = new User ("mario@mia.mail.com", "pass");
        User receiver = new User (targetUser, "pass");

        for (int i = 0; i < writeThreads; i++)
        {
            executor.submit (() -> {
                Email email = new Email (UUID.randomUUID ().toString (), sender, List.of (receiver), "W", "T",
                                         LocalDateTime.now ());
                storageManager.deliver_email (targetUser, email);
                latch.countDown ();
            });
        }

        for (int i = 0; i < readThreads; i++)
        {
            executor.submit (() -> {
                storageManager.load_inbox (targetUser);
                latch.countDown ();
            });
        }

        latch.await ();
        executor.shutdown ();

        List <Email> finalInbox = storageManager.load_inbox (targetUser);
        assertEquals (writeThreads, finalInbox.size ());
    }

    @Test
    void testIndependentUserLocks () throws InterruptedException
    {
        int threadsPerUser = 100;
        ExecutorService executor = Executors.newFixedThreadPool (40);
        CountDownLatch latch = new CountDownLatch (threadsPerUser * 2);

        User sender = new User ("mario@mia.mail.com", "pass");
        User receiver1 = new User (targetUser, "pass");
        User receiver2 = new User (secondUser, "pass");

        for (int i = 0; i < threadsPerUser; i++)
        {
            executor.submit (() -> {
                Email email = new Email (UUID.randomUUID ().toString (), sender, List.of (receiver1), "U1", "T",
                                         LocalDateTime.now ());
                storageManager.deliver_email (targetUser, email);
                latch.countDown ();
            });

            executor.submit (() -> {
                Email email = new Email (UUID.randomUUID ().toString (), sender, List.of (receiver2), "U2", "T",
                                         LocalDateTime.now ());
                storageManager.deliver_email (secondUser, email);
                latch.countDown ();
            });
        }

        latch.await ();
        executor.shutdown ();

        assertEquals (threadsPerUser, storageManager.load_inbox (targetUser).size ());
        assertEquals (threadsPerUser, storageManager.load_inbox (secondUser).size ());
    }
}