package com.example.mail_server.controller;

import com.example.mail_server.model.Server_Model;

public class Server_Controller
{
    private Server_Model model;

    public void set_model (Server_Model model)
    {
        this.model = model;
        /**this.model.latest_Log_Property ().addListener ((_, _, newValue) ->
         {
         if (newValue != null && ! newValue.isEmpty ())
         logArea.appendText (newValue + "\n");
         });*/
    }
}