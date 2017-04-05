/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import se.symsoft.cc2016.logutil.Logged;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("players")
public class PlayerResource {


    @Context
    Application app;

    private DynamoDBMapper getDynamo() {
        return ((SwingitService) app).getDynamoDB();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN"})
    public void list(@Suspended final AsyncResponse asyncResponse) {
        PaginatedScanList<PlayerData> pageList = getDynamo().scan(PlayerData.class, new DynamoDBScanExpression());
        asyncResponse.resume(pageList);
    }

    @GET
    @Path("ping")
    @PermitAll
    public void ping(@Suspended final AsyncResponse asyncResponse) {
        asyncResponse.resume("Pong");
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    @RolesAllowed("ADMIN")
    public void create(@Suspended final AsyncResponse asyncResponse, final PlayerData data) throws URISyntaxException {
        data.setId(UUID.randomUUID());
        try {
            getDynamo().save(data);
            ((SwingitService) app).addPlayer(data);
            asyncResponse.resume(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    @RolesAllowed("ADMIN")
    public void update(@Suspended final AsyncResponse asyncResponse, final PlayerData data) throws URISyntaxException {
        try {
            final PlayerData playerData = getDynamo().load(PlayerData.class, data.getId());
            if (playerData == null) {
                asyncResponse.resume(new NotFoundException("Entity with id = " + data.getId() + " not found"));
                return;
            }
            getDynamo().save(data);
            ((SwingitService) app).addPlayer(data);
            asyncResponse.resume(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }




    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @RolesAllowed("ADMIN")
    public void delete(@Suspended final AsyncResponse asyncResponse, @PathParam("id") UUID id) {
        try {
            final PlayerData playerData = getDynamo().load(PlayerData.class, id);
            if (playerData != null) {
                Map<String, AttributeValue> eav = new HashMap<>();
                eav.put(":val1", new AttributeValue().withS(playerData.getId().toString()));
                PaginatedScanList<RoundData> rounds = getDynamo().scan(RoundData.class, new DynamoDBScanExpression().withIndexName("player-index").
                        withFilterExpression("playerId = :val1")
                        .withExpressionAttributeValues(eav));
                getDynamo().batchDelete(rounds.stream().collect(Collectors.toList()));
                getDynamo().delete(playerData);
                ((SwingitService) app).removePlayer(playerData);
                asyncResponse.resume(playerData);
            } else {
                asyncResponse.resume(new NotFoundException("Entity with id = " + id + " not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }


}
