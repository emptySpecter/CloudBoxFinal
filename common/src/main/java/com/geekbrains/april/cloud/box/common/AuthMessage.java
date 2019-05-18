package com.geekbrains.april.cloud.box.common;

public class AuthMessage implements AbstractMessage {
    private String login;
    private String password;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public AuthMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }
}
