package com.example.shared.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Json_Mapper
{
    private static final ObjectMapper mapper = new ObjectMapper ();

    static
    {
        mapper.registerModule (new JavaTimeModule ());
    }

    public static ObjectMapper get ()
    {
        return mapper;
    }
}