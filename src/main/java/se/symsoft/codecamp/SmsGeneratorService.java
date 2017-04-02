/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.BasicAuthDefinition;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import se.symsoft.cc2016.logutil.RequestLoggingFilter;
import se.symsoft.codecamp.metrics.Metrics;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsGeneratorService extends ResourceConfig {


    private static final String API_VERSION = "0.0.1";
    private final UUID id;
    private Swagger swagger;
    private DynamoDBMapper dynamoDB;
    private AmazonSQSClient sqsClient;
    private String queueUrl;

    public SmsGeneratorService() {
        super(SmsGeneratorResource.class);
        register(RequestLoggingFilter.class);
        register(JacksonJsonProvider.class);
        id = UUID.randomUUID();
    }

    private HttpServer start() throws IOException {
        AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient().withRegion(Regions.EU_WEST_1);
        dynamoDB = new DynamoDBMapper(amazonDynamoDBClient);

        sqsClient = new AmazonSQSClient().withRegion(Regions.EU_WEST_1);
        String queueName = System.getenv("SQS_QUEUE_NAME");
        System.out.println("SQS_QUEUE_NAME = " + queueName);
        GetQueueUrlResult result = sqsClient.getQueueUrl(queueName);
        queueUrl = result.getQueueUrl();
        SqsMessageReceiver sqsMessageReceiver = new SqsMessageReceiver(this, sqsClient,
                queueName);
        new Thread(sqsMessageReceiver).start();

        Metrics.startGraphiteMetricsReporter();

        URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(8070).build();

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, this);
        server.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(SmsGeneratorService.class.getClassLoader(), "web/"),"/generator-ui");
        server.start();
        return  server;
    }

    public DynamoDBMapper getDynamoDB() {
        return dynamoDB;
    }

    public UUID getId() {
        return id;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public AmazonSQSClient getSqsClient() {
        return sqsClient;
    }

    private void setupSwagger() {
        Info info = new Info()
                .title("ECC API")
                .description("Symsoft Enterprise Communications Cloud API")
                .termsOfService("http://symsoft.com/api-terms/")
                .contact(new Contact()
                        .email("info@symsoft.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"))
                .version(API_VERSION);

        // Build a base Swagger. It will be updated in runtime once we are deployed.
        swagger = new Swagger().info(info).basePath("/");
        // todo swagger.addTag(new Tag().name("SIM").description("Operations related to SIM cards"));
        swagger.addTag(new Tag().name("Subscription").description("Operations related to Subscriptions"));
        swagger.addTag(new Tag().name("Service").description("Operations related to Services"));
        swagger.addTag(new Tag().name("SIM").description("Operations related to SIM cards"));
        swagger.addTag(new Tag().name("Order").description("Operations related to Orders"));
        swagger.addTag(new Tag().name("Event").description("Operations related to Event Streams"));
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(1);
        SmsGeneratorService smsGeneratorService = new SmsGeneratorService();
        executor.execute(() -> {
            try {
                smsGeneratorService.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });


    }

}
