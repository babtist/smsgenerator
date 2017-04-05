/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.glassfish.hk2.api.Factory;


class DynamoDbFactory implements Factory<DynamoDBMapper> {
    private final DynamoDBMapper dynamoDB;

    public DynamoDbFactory() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient().withRegion(Regions.EU_WEST_1);
        dynamoDB = new DynamoDBMapper(client);
    }


    @Override
    public DynamoDBMapper provide() {
        return dynamoDB;
    }

    @Override
    public void dispose(DynamoDBMapper dynamoDB) {

    }
}
