package com.hms.stream;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class Messaging {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Messaging.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static Messaging instance;
    
    public Messaging(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        instance = this;
    }

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    @Bean
    public NewTopic mediaUpdatesTopic() {
        return new NewTopic("media-updates", 1, (short) 1);
    }

    @KafkaListener(topics = "media-updates", groupId = "media-stream-service")
    public void listen(String message) {
        LOG.info("Received message: {}", message);
        System.out.println("Received message: " + message);
    }
}
