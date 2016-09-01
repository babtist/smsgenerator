/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsGeneratorService {


    private static final String API_VERSION = "0.0.1";
    private Swagger swagger;

    static HttpServer startServer() throws IOException {

        Metrics.startGraphiteMetricsReporter();

        URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(8070).build();
        ResourceConfig config = new ResourceConfig(SmsGeneratorResource.class, SnsResource.class);
        config.register(RequestLoggingFilter.class);
        config.register(JacksonJsonProvider.class);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(DynamoDbFactory.class).to(DynamoDBMapper.class);
            }
        });

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
        server.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(SmsGeneratorService.class.getClassLoader(), "web/"),"/generator-ui");
        server.start();
        return  server;
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
        executor.execute(() -> {
            try {
                startServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });


    }
}
