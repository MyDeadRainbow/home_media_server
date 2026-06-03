package com.hms.catalog;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class Messaging {
    

    @Bean
    public NewTopic mediaUpdatesTopic() {
        return new NewTopic("media-updates", 1, (short) 1);
    }

    @KafkaListener(topics = "media-updates", groupId = "media-catalog-service")
    public void listen(String message) {
        System.out.println("Received message: " + message);
    }
}
