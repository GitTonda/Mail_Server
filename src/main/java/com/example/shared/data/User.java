package com.example.shared.data;

import java.io.Serializable;

public record User
        (
                String username,
                String password
        ) implements Serializable {}
