package com.hms.acquisition.datamine;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.hms.shared.messaging.metadata.MetaData;
import com.hms.shared.messaging.metadata.MetaDataSerializer;

@Service
public class MetaDataProducer {

    @Qualifier("metaDataKafkaTemplate")
    private final KafkaTemplate<String, MetaData> kafkaTemplate;

    public static MetaDataProducer INSTANCE;

    public MetaDataProducer(KafkaTemplate<String, MetaData> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        INSTANCE = this;
    }

    public void sendMessage(MetaData message) {
        sendMessage(MetaData.TOPIC, message);
    }

    public void sendMessage(String topic, MetaData message) {
        Preconditions.checkNotNull(topic, "Topic is not initialized in MetaDataProducer");
        Preconditions.checkNotNull(kafkaTemplate, "KafkaTemplate is not initialized in MetaDataProducer");
        kafkaTemplate.send(topic, message);
    }

    public static void postMessage(MetaData message) {
        Preconditions.checkNotNull(message, "MetaData message cannot be null");
        Preconditions.checkNotNull(INSTANCE, "MetaDataProducer service is not initialized yet");
        INSTANCE.sendMessage(message);
    }

}

@Configuration
class MetaDataProducerConfig {

    @Bean
    public NewTopic metaDataTopic() {
        return new NewTopic(MetaData.TOPIC, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, MetaData> metaDataProducerFactory() {
        Map<String, Object> configProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MetaDataSerializer.class.getName());
        Preconditions.checkNotNull(configProps, "Failed to create Kafka producer configuration for MetaData");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, MetaData> metaDataKafkaTemplate() {
        ProducerFactory<String, MetaData> factory = metaDataProducerFactory();
        Preconditions.checkNotNull(factory, "Failed to create Kafka factory for MetaData");
        return new KafkaTemplate<>(factory);
    }
}