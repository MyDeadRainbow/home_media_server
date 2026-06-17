package com.hms.stream.messaging;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateSerializer;

@Service
public class CatalogUpdateProducer {
    // private static final Logger LOG = LoggerFactory.getLogger(CatalogUpdateProducer.class);

    @Qualifier("catalogUpdateKafkaTemplate")
    private final KafkaTemplate<String, CatalogUpdate> kafkaTemplate;

    public static CatalogUpdateProducer INSTANCE;

    public CatalogUpdateProducer(KafkaTemplate<String, CatalogUpdate> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        INSTANCE = this;
    }

    public void sendMessage(CatalogUpdate message) {
        sendMessage(CatalogUpdate.TOPIC, message);
    }

    public void sendMessage(String topic, CatalogUpdate message) {
        Preconditions.checkNotNull(topic, "Topic is not initialized in CatalogUpdates");
        Preconditions.checkNotNull(kafkaTemplate, "KafkaTemplate is not initialized in CatalogUpdates");
        kafkaTemplate.send(topic, message);
        kafkaTemplate.flush();
    }

    public static void postMessage(CatalogUpdate message) {
        Preconditions.checkNotNull(message, "CatalogUpdate message cannot be null");
        Preconditions.checkNotNull(INSTANCE, "CatalogUpdates service is not initialized yet");
        INSTANCE.sendMessage(message);
    }

}

@Configuration
class CatalogUpdateProducerConfig {

    @Bean
    public NewTopic catalogUpdatesTopic() {
        return new NewTopic(CatalogUpdate.TOPIC, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, CatalogUpdate> catalogUpdateProducerFactory() {
        Map<String, Object> configProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CatalogUpdateSerializer.class.getName());
        Preconditions.checkNotNull(configProps, "Failed to create Kafka producer configuration for CatalogUpdates");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CatalogUpdate> catalogUpdateKafkaTemplate() {
        ProducerFactory<String, CatalogUpdate> factory = catalogUpdateProducerFactory();
        Preconditions.checkNotNull(factory, "Failed to create Kafka factory for CatalogUpdates");
        return new KafkaTemplate<>(factory);
    }
}