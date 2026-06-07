package com.hms.stream.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class MediaUpdates {
    private static final Logger LOG = LoggerFactory.getLogger(MediaUpdates.class);

    public static final String TOPIC = "media-updates";

    private final KafkaTemplate<String, String> kafkaTemplate;
    public static MediaUpdates INSTANCE;
    
    private MediaUpdates(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        INSTANCE = this;
    }

    public void sendMessage(String message) {
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    @Bean
    public NewTopic mediaUpdatesTopic() {
        return new NewTopic(TOPIC, 1, (short) 1);
    }

    @KafkaListener(topics = TOPIC, groupId = "media-stream-service")
    public void listen(String message) {
        LOG.info("Received message: {}", message);
        System.out.println("Received message: " + message);
    }
}
