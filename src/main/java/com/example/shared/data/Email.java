package com.example.shared.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record Email
        (
                String id,
                User sender,
                List <User> receivers,
                String subject,
                String text,
                LocalDateTime date
        ) implements Serializable {}
