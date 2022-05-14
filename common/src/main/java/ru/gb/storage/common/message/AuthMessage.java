package ru.gb.storage.common.message;

public class AuthMessage extends Message {
    private String login;
    private String password;

    public String getPassword() {
        return password;
    }

    public String getLogin() {
        return login;
    }

    public void setAuth(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public String toString() {
        return "AuthMessage{ \n" +
                "login= " + login + '\n' +
                "password= " + password + '\n' +
                '}';
    }
}
