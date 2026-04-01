package com.example.mail_server.app;

import com.example.mail_server.controller.Server_Controller;
import com.example.mail_server.model.Server_Model;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Server_App extends Application
{
    private static final String PATH = "/com/example/mail_server/server_gui.fxml";
    private Server_Model model;

    @Override
    public void start (Stage stage) throws IOException
    {
        // Model
        model = new Server_Model ();
        model.start_Server ();

        // GUI and Controller
        FXMLLoader fxmlLoader = new FXMLLoader (Server_App.class.getResource (PATH));
        Scene scene = new Scene (fxmlLoader.load (), 1000, 700);
        scene.getStylesheets ().add (
                Objects.requireNonNull (getClass ().getResource ("/com/example/mail_server/server_styles.css"))
                        .toExternalForm ()
        );
        Server_Controller controller = fxmlLoader.getController ();
        controller.set_model (model);
        stage.setOnCloseRequest (_ -> {
            javafx.application.Platform.exit ();
            System.exit (0);
        });

        // Show UI
        stage.setTitle ("Mail Server");
        stage.setScene (scene);
        stage.show ();
    }

    @Override
    public void stop () throws Exception
    {
        if (model != null) model.stop_Server ();
        super.stop ();
    }
}
