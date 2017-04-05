/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@DynamoDBTable(tableName="Player")
@Getter
@Setter
public class PlayerData {
    private String name;
    @DynamoDBHashKey(attributeName="id")
    private UUID id;
    private String username;
    private String password;
    private String role;

}
