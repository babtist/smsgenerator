/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import se.symsoft.codecamp.metrics.Metrics;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SmsGenerator {
    private final GeneratorData data;
    private boolean stopped = false;
    List<Thread> threads = new ArrayList<>();

    private final DynamoDBMapper dynamoDB;
    private String sendCounterName;
    private String successCounterName;
    private String failedCounterName;

    private String sendMeterName;
    private String successMeterName;
    private String failedMeterName;

    private AtomicInteger success = new AtomicInteger();
    private AtomicInteger failed = new AtomicInteger();

    public SmsGenerator(GeneratorData data, DynamoDBMapper dynamoDB) {
        this.data = data;
        this.dynamoDB = dynamoDB;
        this.sendCounterName = this.data.getDescription() + ".submit";
        this.successCounterName = this.data.getDescription() + ".submit.success";
        this.failedCounterName = this.data.getDescription() + ".submit.failed";

        this.sendMeterName = this.data.getDescription() + ".submit.rate";
        this.successMeterName = this.data.getDescription() + ".submit.success.rate";
        this.failedMeterName = this.data.getDescription() + ".submit.failed.rate";

    }

    public void start() {
        System.out.println("SMS generator "+data.getDescription()+ " started!");
        int numberOfWorkers = data.getRate() / 20 + 1;
        final int sleepTime = 1000 * numberOfWorkers / data.getRate();
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(JacksonJsonProvider.class);
        final Client client = ClientBuilder.newClient(clientConfig);
        long stopTime = System.currentTimeMillis() + data.getDuration()*1000;

        for (int i = 0; i < numberOfWorkers; i++) {
            Thread t = new Thread(() -> {
                while (stopTime > System.currentTimeMillis() && !stopped) {
                    sendSms(client, data.getDeliveryUrl(), createSms());
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            threads.add(t);
            t.start();
        }
        new Thread(new StateChangeDetectionTask(stopTime)).start();
        for (Thread t : threads) {
            try {
                t.join(data.getDuration()*1000 + 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stopped = true;
        data.setSuccess(success.get());
        data.setFailed(failed.get());
        data.setState(0);
        dynamoDB.save(data);
        System.out.println("SMS generator "+data.getDescription()+ " ended");

    }

    private void sendSms(Client client, String deliveryUrl, SmsSubmit sms) {
        System.out.println("send sms");
        Metrics.METRIC_REGISTRY.meter(sendMeterName).mark();
        Metrics.METRIC_REGISTRY.counter(sendCounterName).inc();

        WebTarget webTarget = client.target(deliveryUrl);
        webTarget.request().buildPost(Entity.json(sms)).submit(new InvocationCallback<SmsResponse>() {
            @Override
            public void completed(SmsResponse smsResponse) {
                if (smsResponse.getResponseCode() == 0) {
                    deliverySucceeded();
                } else {
                    deliveryFailed();
                }
            }

            @Override
            public void failed(Throwable throwable) {
                throwable.printStackTrace();
                deliveryFailed();
            }
        });
    }

    private SmsSubmit createSms() {
        //@todo support random numbers
        return new SmsSubmit("46702692431", "46702692432", "AWS rocks");
    }

    private void deliverySucceeded() {
        Metrics.METRIC_REGISTRY.counter(successCounterName).inc();
        Metrics.METRIC_REGISTRY.meter(successMeterName).mark();
        success.incrementAndGet();
    }

    private void deliveryFailed() {
        Metrics.METRIC_REGISTRY.counter(failedCounterName).inc();

        Metrics.METRIC_REGISTRY.meter(failedMeterName).mark();
        failed.incrementAndGet();
    }

    class StateChangeDetectionTask implements Runnable {
        private long stopTime;

        public StateChangeDetectionTask(long stopTime) {
            this.stopTime = stopTime;
        }

        @Override
        public void run() {
            while (stopTime > System.currentTimeMillis() && !stopped) {
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final GeneratorData generatorData = dynamoDB.load(GeneratorData.class, data.getId());
                if (generatorData == null || generatorData.getState() == GeneratorData.STATE_IDLE || generatorData.getState() == GeneratorData.STATE_STOPPING) {
                    stopped = true;
                } else {
                    generatorData.setSuccess(success.get());
                    generatorData.setFailed(failed.get());
                    dynamoDB.save(generatorData);
                }
            }
        }
    }


}
