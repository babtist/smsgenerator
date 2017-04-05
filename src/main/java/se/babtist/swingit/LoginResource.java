/*
 * Copyright Symsoft AB 1996-2017. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;

import javax.annotation.security.PermitAll;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("login")
public class LoginResource {
    private static final long THIRTY_MINUTES = 30*60*1000;

    @Context
    Application app;

    private DynamoDBMapper getDynamo() {
        return ((SwingitService) app).getDynamoDB();
    }

    @POST
    @PermitAll
    public void login(@Suspended final AsyncResponse asyncResponse, CredentialData credentials) {
        try {

            // Authenticate the user using the credentials provided
            Optional<PlayerData> optionalPlayer = authenticate(credentials);
            if (!optionalPlayer.isPresent()) {
                asyncResponse.resume(new WebApplicationException(Response.Status.UNAUTHORIZED));
                return;
            }

            // Issue a token for the user
            TokenData token = issueToken(optionalPlayer.get());

            // Return the token on the response
            asyncResponse.resume(token);

        } catch (Exception e) {
            asyncResponse.resume(new WebApplicationException(Response.Status.UNAUTHORIZED));
        }
    }

    private Optional<PlayerData> authenticate(CredentialData data) {
        //@todo This can be optimized so we don't scan whole table
        PaginatedScanList<PlayerData> playerList = getDynamo().scan(PlayerData.class, new DynamoDBScanExpression());
        return playerList.stream().filter(p -> p.getUsername().equals(data.getName()) && p.getPassword().equals(data.getPassword())).findFirst();
    }

    private TokenData issueToken(PlayerData data) {
        return new TokenData(JwtUtil.createJWT(data.getId().toString(), "Babben", data.getRole(), System.currentTimeMillis() + THIRTY_MINUTES),
                data.getRole(), data.getName(), data.getId());
    }
}
