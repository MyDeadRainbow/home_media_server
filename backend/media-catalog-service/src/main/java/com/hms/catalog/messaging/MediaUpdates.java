package com.hms.catalog.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.hms.shared.messaging.mediaupdates.MediaUpdate;

// @Component
// @Configuration
// public class MediaUpdates {
//     private static final Logger LOG = LoggerFactory.getLogger(MediaUpdates.class);

//     public static final String TOPIC = "media-updates";

//     private final KafkaTemplate<String, MediaUpdate> kafkaTemplate;
//     public static MediaUpdates INSTANCE;
    
//     public MediaUpdates(KafkaTemplate<String, MediaUpdate> kafkaTemplate) {
//         this.kafkaTemplate = kafkaTemplate;
//         INSTANCE = this;
//     }

//     public void sendMessage(MediaUpdate message) {
//         kafkaTemplate.send(TOPIC, message);
//     }

//     public void sendMessage(String topic, MediaUpdate message) {        
//         kafkaTemplate.send(topic, message);
//     }

//     public static void postMessage(MediaUpdate message) {
//         if (INSTANCE != null) {
//             INSTANCE.sendMessage(message);
//         } else {
//             LOG.warn("MediaUpdates instance is not initialized yet. Message not sent: {}", message);
//         }
//     }

//     @Bean
//     public NewTopic mediaUpdatesTopic() {
//         return new NewTopic(TOPIC, 1, (short) 1);
//     }

//     @KafkaListener(topics = TOPIC, groupId = "media-catalog-service")
//     public void listen(MediaUpdate message) {
//         LOG.info("Received message: {}", message);
//         System.out.println("Received message: " + message);
//     }
// }
