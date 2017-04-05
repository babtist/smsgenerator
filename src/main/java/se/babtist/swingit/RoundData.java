/*
 * Copyright Symsoft AB 1996-2017. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@DynamoDBTable(tableName="Round")
@Getter
@Setter
public class RoundData {
    @DynamoDBHashKey(attributeName="id")
    private UUID id;
    private String date;
    private UUID playerId;
    private String playerName;
    private BigDecimal hcp;
    private BigDecimal score;


}
