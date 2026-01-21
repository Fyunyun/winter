package com.winter.model;

public class AccountModel {

    private String username;
    private String password;
    private long playerId;
    public AccountModel(String username, String password, long playerId) {
        this.username = username;
        this.password = password;
        this.playerId = playerId;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public long getPlayerId() {
        return playerId;
    }
}
