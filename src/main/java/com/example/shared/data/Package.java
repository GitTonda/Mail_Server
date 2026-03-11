package com.example.shared.data;

import java.io.Serializable;
import java.util.List;

public record Package
        (
                TYPE type,
                User user,
                Email email,
                List <Email> email_list,
                String message
        ) implements Serializable {}