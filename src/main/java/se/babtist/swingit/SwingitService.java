/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.babtist.swingit;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import se.symsoft.cc2016.logutil.RequestLoggingFilter;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SwingitService extends ResourceConfig {


    private final UUID id;
    private DynamoDBMapper dynamoDB;
    private Map<UUID, String> playerMap = Collections.synchronizedMap(new HashMap<>());

    public SwingitService() {
        super(PlayerResource.class, RoundResource.class, LeaderboardResource.class, LoginResource.class);

        register(RequestLoggingFilter.class);
        register(JacksonJsonProvider.class);
        register(AuthenticationFilter.class);
        id = UUID.randomUUID();
    }

    private HttpServer start() throws IOException {
        AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient().withRegion(Regions.EU_WEST_1);
        dynamoDB = new DynamoDBMapper(amazonDynamoDBClient);

        //Metrics.startGraphiteMetricsReporter();

        URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(8070).build();

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, this);
        server.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(SwingitService.class.getClassLoader(), "web/"),"/swingit");
        server.start();

        PaginatedScanList<PlayerData> players = dynamoDB.scan(PlayerData.class, new DynamoDBScanExpression());
        players.forEach(p -> playerMap.put(p.getId(), p.getName()));

        return  server;
    }

    public DynamoDBMapper getDynamoDB() {
        return dynamoDB;
    }

    public UUID getId() {
        return id;
    }


    public void addPlayer(PlayerData playerData) {
        playerMap.put(playerData.getId(), playerData.getName());
    }

    public void removePlayer(PlayerData playerData) {
        playerMap.remove(playerData.getId());
    }

    public String getPlayerName(UUID id) {
        return playerMap.get(id);
    }



    public static void main(String[] args) throws IOException, InterruptedException {


        ExecutorService executor = Executors.newFixedThreadPool(1);
        SwingitService smsGeneratorService = new SwingitService();
        executor.execute(() -> {
            try {
                smsGeneratorService.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });


    }

}
