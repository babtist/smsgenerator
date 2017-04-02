/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class SqsMessageReceiver implements Runnable {
    private final SmsGeneratorService service;
    private final AmazonSQSClient sqs;
    private final String queueName;
    private boolean running;

    public SqsMessageReceiver(SmsGeneratorService service, AmazonSQSClient sqs, String queueName) {
        this.service = service;
        this.sqs = sqs;
        this.queueName = queueName;
    }

    public void run() {
        running = true;
        while(running) {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueName).
                    withMaxNumberOfMessages(10).withWaitTimeSeconds(10);
            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

            for (Message message : messages) {
                System.out.println("    Body:          " + message.getBody());
                // We must delete the message from the queue
                sqs.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueName).withReceiptHandle(message.getReceiptHandle()));

                try {
                    startGenerator(new ObjectMapper().readValue(message.getBody(), GeneratorData.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void stop() {
        running = false;
    }

    private void startGenerator(GeneratorData data) {
        SmsGenerator generator = new SmsGenerator(data, service.getId(), service.getDynamoDB());
        new Thread(generator::start).start();
    }
}
