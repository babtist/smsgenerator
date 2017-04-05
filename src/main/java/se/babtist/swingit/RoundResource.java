/*
 * Copyright Symsoft AB 1996-2017. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import se.symsoft.cc2016.logutil.Logged;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import javax.ws.rs.core.SecurityContext;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Path("rounds")
public class RoundResource {
    @Context
    Application app;

    private DynamoDBMapper getDynamo() {
        return ((SwingitService) app).getDynamoDB();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "USER", "GUEST"})
    public void list(@Suspended final AsyncResponse asyncResponse) {
        PaginatedScanList<RoundData> pageList = getDynamo().scan(RoundData.class, new DynamoDBScanExpression());
        List<RoundData> decorated = new ArrayList<>();
        pageList.stream().forEach(r -> {
            r.setPlayerName(((SwingitService) app).getPlayerName(r.getPlayerId()));
            decorated.add(r);
        });
        asyncResponse.resume(decorated);
    }

    @GET
    @Path("ping")
    public void ping(@Suspended final AsyncResponse asyncResponse) {
        asyncResponse.resume("Pong");
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    @RolesAllowed({"ADMIN", "USER"})
    public void create(@Suspended final AsyncResponse asyncResponse, final RoundData data) throws URISyntaxException {
        data.setId(UUID.randomUUID());
        String date = data.getDate();
        int index = date.indexOf("T");
        if (index > 0) {
            date = date.substring(0, index);
        }
        data.setDate(date);
        try {
            getDynamo().save(data);

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
    @RolesAllowed({"ADMIN", "USER"})
    public void update(@Suspended final AsyncResponse asyncResponse, final RoundData data) throws URISyntaxException {
        try {
            final RoundData RoundData = getDynamo().load(RoundData.class, data.getId());
            if (RoundData == null) {
                asyncResponse.resume(new NotFoundException("Entity with id = " + data.getId() + " not found"));
                return;
            }
            String date = data.getDate();
            int index = date.indexOf("T");
            if (index > 0) {
                date = date.substring(0, index);
            }
            data.setDate(date);
            getDynamo().save(data);

            asyncResponse.resume(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }




    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    @RolesAllowed({"ADMIN", "USER"})
    public void delete(@Suspended final AsyncResponse asyncResponse, @PathParam("id") UUID id) {
        try {
            final RoundData RoundData = getDynamo().load(RoundData.class, id);
            if (RoundData != null) {
                getDynamo().delete(RoundData);
                asyncResponse.resume(RoundData);
            } else {
                asyncResponse.resume(new NotFoundException("Entity with id = " + id + " not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

}
