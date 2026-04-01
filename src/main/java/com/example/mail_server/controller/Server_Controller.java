package com.example.mail_server.controller;

import com.example.mail_server.model.Server_Model;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class Server_Controller
{
    @FXML
    private TextArea text_area_info;
    @FXML
    private ScrollPane scroll_pane_logs;
    @FXML
    private TextFlow text_flow_logs;

    public void set_model (Server_Model model)
    {
        model.get_log ().addListener ((_, _, newValue) -> {
            if (newValue != null && ! newValue.isEmpty ()) add_log (newValue);
        });

        model.server_info_Property ().addListener ((_, _, newValue) -> {
            if (newValue != null) text_area_info.setText (newValue);
        });
    }

    @FXML
    public void on_clear_logs_clicked ()
    {
        text_flow_logs.getChildren ().clear ();
        add_log ("[INFO] Logs cleared");
    }

    public void add_log (String message)
    {
        Platform.runLater (() -> {
            if (text_flow_logs != null)
            {
                Text text = new Text (message + "\n");

                text.setStyle ("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 14px;");

                if (message.contains ("[REQ]"))
                {
                    text.setFill (Color.web ("#a0aec0"));
                }
                else if (message.contains ("[ACTION]") || message.contains ("[AUTH]"))
                {
                    text.setFill (Color.web ("#38a169"));
                    text.setStyle (text.getStyle () + "-fx-font-weight: bold;");
                }
                else if (message.contains ("[INFO]"))
                {
                    text.setFill (Color.web ("#3182ce"));
                }
                else if (message.contains ("[ERROR]"))
                {
                    text.setFill (Color.web ("#e53e3e"));
                    text.setStyle (text.getStyle () + "-fx-font-weight: bold;");
                }
                else
                {
                    text.setFill (Color.web ("#102a43"));
                }

                text_flow_logs.getChildren ().add (text);
                scroll_pane_logs.layout ();
                scroll_pane_logs.setVvalue (1.0);
            }
        });
    }
}