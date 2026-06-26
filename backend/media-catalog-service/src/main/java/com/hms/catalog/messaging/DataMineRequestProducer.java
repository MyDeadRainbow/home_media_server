package com.hms.catalog.messaging;

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
import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequestSerializer;

@Service
public class DataMineRequestProducer {
    
    @Qualifier("dataMineRequestKafkaTemplate")
    private final KafkaTemplate<String, DataMineRequest> kafkaTemplate;

    public static DataMineRequestProducer INSTANCE;

    public DataMineRequestProducer(KafkaTemplate<String, DataMineRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        INSTANCE = this;
    }

    public void sendMessage(DataMineRequest message) {
        sendMessage(DataMineRequest.TOPIC, message);
    }

    public void sendMessage(String topic, DataMineRequest message) {
        Preconditions.checkNotNull(topic, "Topic is not initialized in DataMineRequests");
        Preconditions.checkNotNull(kafkaTemplate, "KafkaTemplate is not initialized in DataMineRequests");
        kafkaTemplate.send(topic, message);
    }

    public static void postMessage(DataMineRequest message) {
        Preconditions.checkNotNull(message, "DataMineRequest message cannot be null");
        Preconditions.checkNotNull(INSTANCE, "DataMineRequests service is not initialized yet");
        INSTANCE.sendMessage(message);
    }

}

@Configuration
class DataMineRequestProducerConfig {

    @Bean
    public NewTopic dataMineRequestsTopic() {
        return new NewTopic(DataMineRequest.TOPIC, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, DataMineRequest> dataMineRequestProducerFactory() {
        Map<String, Object> configProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DataMineRequestSerializer.class.getName());
        Preconditions.checkNotNull(configProps, "Failed to create Kafka producer configuration for DataMineRequests");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, DataMineRequest> dataMineRequestKafkaTemplate() {
        ProducerFactory<String, DataMineRequest> factory = dataMineRequestProducerFactory();
        Preconditions.checkNotNull(factory, "Failed to create Kafka factory for DataMineRequests");
        return new KafkaTemplate<>(factory);
    }
}