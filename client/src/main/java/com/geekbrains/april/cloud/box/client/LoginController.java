package com.geekbrains.april.cloud.box.client;

import com.geekbrains.april.cloud.box.common.AuthMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    VBox globParent;

    String user = "";

    public void auth(ActionEvent actionEvent) {
        user = login.getText();
        Network.sendMsg(new AuthMessage(user, password.getText()));
    }
}
