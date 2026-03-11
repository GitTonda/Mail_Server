module com.example.mail_server {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;


    opens com.example.mail_server to javafx.fxml;
    exports com.example.mail_server;
    exports com.example.mail_server.model;
    opens com.example.mail_server.model to javafx.fxml;
    exports com.example.mail_server.controller;
    opens com.example.mail_server.controller to javafx.fxml;
    exports com.example.mail_server.app;
    opens com.example.mail_server.app to javafx.fxml;
    exports com.example.shared.data;
    opens com.example.shared.data to javafx.fxml;
}