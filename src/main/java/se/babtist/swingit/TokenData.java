/*
 * Copyright Symsoft AB 1996-2017. All Rights Reserved.
 */
package se.babtist.swingit;

import java.util.UUID;

public class TokenData {
    private String token;
    private String role;
    private String playerName;
    private UUID playerid;

    public TokenData(String token, String role, String playerName, UUID playerid) {
        this.token = token;
        this.role = role;
        this.playerName = playerName;
        this.playerid = playerid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public UUID getPlayerid() {
        return playerid;
    }

    public void setPlayerid(UUID playerid) {
        this.playerid = playerid;
    }
}
