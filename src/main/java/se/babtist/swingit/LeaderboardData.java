/*
 * Copyright Symsoft AB 1996-2017. All Rights Reserved.
 */
package se.babtist.swingit;

import java.math.BigDecimal;
import java.util.UUID;

public class LeaderboardData {
    private String playerName;
    private BigDecimal score;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
