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
    private Server_Model model;

    @FXML
    private TextArea text_area_info;

    // --- NEW LOG COMPONENTS ---
    @FXML
    private ScrollPane scroll_pane_logs;
    @FXML
    private TextFlow text_flow_logs;

    public void set_model (Server_Model model)
    {
        this.model = model;

        this.model.get_Log ().addListener ((observable, oldValue, newValue) -> {
            if (newValue != null && ! newValue.isEmpty ())
            {
                add_log (newValue); // Route through our new color-coding method
            }
        });

        this.model.server_info_Property ().addListener ((observable, oldValue, newValue) -> {
            if (newValue != null)
            {
                text_area_info.setText (newValue);
            }
        });
    }

    @FXML
    public void on_clear_logs_clicked ()
    {
        text_flow_logs.getChildren ().clear (); // Clear the TextFlow
        add_log ("Logs cleared.");
    }

    /**
     * Parses the log message, assigns a color, and appends it to the console.
     */
    public void add_log (String message)
    {
        Platform.runLater (() -> {
            if (text_flow_logs != null)
            {
                Text text = new Text (message + "\n");

                // Base font styling to match the rest of the console
                text.setStyle ("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 14px;");

                // --- COLOR PARSING LOGIC ---
                if (message.contains ("[REQ]"))
                {
                    // Mute routine polling to soft gray
                    text.setFill (Color.web ("#a0aec0"));
                }
                else if (message.contains ("[ACTION]") || message.contains ("[AUTH]"))
                {
                    // Successes and Logins are bold green
                    text.setFill (Color.web ("#38a169"));
                    text.setStyle (text.getStyle () + "-fx-font-weight: bold;");
                }
                else if (message.contains ("[INFO]"))
                {
                    // General info and connections are bright blue
                    text.setFill (Color.web ("#3182ce"));
                }
                else if (message.contains ("[ERROR]"))
                {
                    // Errors are bold red
                    text.setFill (Color.web ("#e53e3e"));
                    text.setStyle (text.getStyle () + "-fx-font-weight: bold;");
                }
                else
                {
                    // Default fallback color (Deep Navy)
                    text.setFill (Color.web ("#102a43"));
                }

                // Add the colored text to the view
                text_flow_logs.getChildren ().add (text);

                // Force the scroll pane to snap to the bottom so we always see the newest logs
                scroll_pane_logs.layout ();
                scroll_pane_logs.setVvalue (1.0);
            }
        });
    }

    public void update_info (String info)
    {
        Platform.runLater (() -> {
            if (text_area_info != null)
            {
                text_area_info.setText (info);
            }
        });
    }
}