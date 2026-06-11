package com.hms.shared.messaging.catalogupdates;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Qualifier;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Service;

// import com.google.common.base.Preconditions;

// // @Service
// // public class CatalogUpdateProducer {
// //     public static final String TOPIC = "catalog-updates";

// //     @Autowired
// //     @Qualifier("catalogUpdateKafkaTemplate")
// //     private KafkaTemplate<String, CatalogUpdate> kafkaTemplate;

// //     public static CatalogUpdateProducer INSTANCE;

// //     public CatalogUpdateProducer() {
// //         INSTANCE = this;
// //     }

// //     public void sendMessage(CatalogUpdate message) {
// //         sendMessage(TOPIC, message);
// //     }

// //     public void sendMessage(String topic, CatalogUpdate message) {
// //         Preconditions.checkNotNull(topic, "Topic is not initialized in CatalogUpdates");
// //         Preconditions.checkNotNull(kafkaTemplate, "KafkaTemplate is not initialized in CatalogUpdates");
// //         kafkaTemplate.send(topic, message);
// //     }

// //     public static void postMessage(CatalogUpdate message) {
// //         Preconditions.checkNotNull(message, "CatalogUpdate message cannot be null");
// //         Preconditions.checkNotNull(INSTANCE, "CatalogUpdates service is not initialized yet");
// //         INSTANCE.sendMessage(message);
// //     }
// // }
