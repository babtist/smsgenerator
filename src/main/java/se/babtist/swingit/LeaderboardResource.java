/*
 * Copyright Symsoft AB 1996-2017. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("leaderboard")
public class LeaderboardResource {

    @Context
    Application app;

    private DynamoDBMapper getDynamo() {
        return ((SwingitService) app).getDynamoDB();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "USER", "GUEST"})
    public void list(@Suspended final AsyncResponse asyncResponse) {
        try {
            PaginatedScanList<PlayerData> pageList = getDynamo().scan(PlayerData.class, new DynamoDBScanExpression());
            List<LeaderboardData> board = new ArrayList<>();
            for (PlayerData player : pageList) {
                Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
                eav.put(":val1", new AttributeValue().withS(player.getId().toString()));
                DynamoDBQueryExpression<RoundData> spec = new DynamoDBQueryExpression<RoundData>()
                        .withKeyConditionExpression("playerId = :val1").withExpressionAttributeValues(eav).
                                withIndexName("player-index");
                spec.setConsistentRead(false);
                ;
                PaginatedQueryList<RoundData> rounds = getDynamo().query(RoundData.class, spec);

                List<RoundData> sortedList = rounds.stream().sorted(new Comparator<RoundData>() {
                    @Override
                    public int compare(RoundData o1, RoundData o2) {
                        return o1.getScore().compareTo(o2.getScore());
                    }
                }).limit(3).collect(Collectors.toList());
                double score;
                if (sortedList.size() < 3) {
                    score = 54 * (3 - sortedList.size());
                } else {
                    score = 0;
                }
                score = score + sortedList.stream().mapToDouble(r -> r.getScore().doubleValue()).sum();

                LeaderboardData data = new LeaderboardData();
                data.setPlayerName(player.getName());
                data.setScore(new BigDecimal(score / 3).setScale(2, BigDecimal.ROUND_FLOOR));
                board.add(data);
            }
            asyncResponse.resume(board.stream().sorted(new Comparator<LeaderboardData>() {
                @Override
                public int compare(LeaderboardData o1, LeaderboardData o2) {
                    return o1.getScore().compareTo(o2.getScore());
                }
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
