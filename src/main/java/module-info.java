module com.example.mail_server {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.mail_server to javafx.fxml;
    exports com.example.mail_server;
}