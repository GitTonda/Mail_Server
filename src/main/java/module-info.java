module com.example.mail_server {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.mail_server to javafx.fxml;
    exports com.example.mail_server;
    exports com.example.mail_server.model;
    opens com.example.mail_server.model to javafx.fxml;
    exports com.example.mail_server.controller;
    opens com.example.mail_server.controller to javafx.fxml;
    exports com.example.mail_server.app;
    opens com.example.mail_server.app to javafx.fxml;
}