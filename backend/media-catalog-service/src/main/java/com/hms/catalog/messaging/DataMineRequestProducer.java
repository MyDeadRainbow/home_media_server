package com.hms.catalog.messaging;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;
import com.hms.shared.media.serialize.MovieSerializer;
import com.hms.shared.media.serialize.SeriesSerializer;
import com.hms.shared.messaging.Topics;
import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequestSerializer;

// @Service
// public class DataMineRequestProducer {
    
//     @Qualifier("dataMineRequestKafkaTemplate")
//     private final KafkaTemplate<String, DataMineRequest> kafkaTemplate;

//     public static DataMineRequestProducer INSTANCE;

//     public DataMineRequestProducer(KafkaTemplate<String, DataMineRequest> kafkaTemplate) {
//         this.kafkaTemplate = kafkaTemplate;
//         INSTANCE = this;
//     }

//     public void sendMessage(DataMineRequest message) {
//         sendMessage(DataMineRequest.TOPIC, message);
//     }

//     public void sendMessage(String topic, DataMineRequest message) {
//         Preconditions.checkNotNull(topic, "Topic is not initialized in DataMineRequests");
//         Preconditions.checkNotNull(kafkaTemplate, "KafkaTemplate is not initialized in DataMineRequests");
//         kafkaTemplate.send(topic, message);
//     }

//     public static void postMessage(DataMineRequest message) {
//         Preconditions.checkNotNull(message, "DataMineRequest message cannot be null");
//         Preconditions.checkNotNull(INSTANCE, "DataMineRequests service is not initialized yet");
//         INSTANCE.sendMessage(message);
//     }

// }

// @Configuration
// class DataMineRequestProducerConfig {

//     @Bean
//     public NewTopic dataMineRequestsTopic() {
//         return new NewTopic(DataMineRequest.TOPIC, 1, (short) 1);
//     }

//     @Bean
//     public ProducerFactory<String, DataMineRequest> dataMineRequestProducerFactory() {
//         Map<String, Object> configProps = Map.of(
//                 ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
//                 ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
//                 ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DataMineRequestSerializer.class.getName());
//         Preconditions.checkNotNull(configProps, "Failed to create Kafka producer configuration for DataMineRequests");
//         return new DefaultKafkaProducerFactory<>(configProps);
//     }

//     @Bean
//     public KafkaTemplate<String, DataMineRequest> dataMineRequestKafkaTemplate() {
//         ProducerFactory<String, DataMineRequest> factory = dataMineRequestProducerFactory();
//         Preconditions.checkNotNull(factory, "Failed to create Kafka factory for DataMineRequests");
//         return new KafkaTemplate<>(factory);
//     }
// }

@Service
public class DataMineRequestProducer {

    @Qualifier("movieKafkaTemplate")
    private final KafkaTemplate<String, Movie> movieKafkaTemplate;

    @Qualifier("seriesKafkaTemplate")
    private final KafkaTemplate<String, Series> seriesKafkaTemplate;

    public static DataMineRequestProducer INSTANCE;

    public DataMineRequestProducer(KafkaTemplate<String, Movie> movieKafkaTemplate, KafkaTemplate<String, Series> seriesKafkaTemplate) {
        this.movieKafkaTemplate = movieKafkaTemplate;
        this.seriesKafkaTemplate = seriesKafkaTemplate;
        INSTANCE = this;
    }

    public void sendMessage(Movie message) {
        sendMessage(Topics.DATAMINE_MOVIE, message);
    }

    public void sendMessage(String topic, Movie message) {
        Preconditions.checkNotNull(topic, "Topic is not initialized in DataMineRequestProducer");
        Preconditions.checkNotNull(movieKafkaTemplate, "KafkaTemplate is not initialized in DataMineRequestProducer");
        movieKafkaTemplate.send(topic, message);
    }

    public static void postMessage(Movie message) {
        Preconditions.checkNotNull(message, "DataMineRequest message cannot be null");
        Preconditions.checkNotNull(INSTANCE, "DataMineRequestProducer service is not initialized yet");
        INSTANCE.sendMessage(message);
    }

    public void sendMessage(Series message) {
        sendMessage(Topics.DATAMINE_SERIES, message);
    }

    public void sendMessage(String topic, Series message) {
        Preconditions.checkNotNull(topic, "Topic is not initialized in DataMineRequestProducer");
        Preconditions.checkNotNull(seriesKafkaTemplate, "KafkaTemplate is not initialized in DataMineRequestProducer");
        seriesKafkaTemplate.send(topic, message);
    }

    public static void postMessage(Series message) {
        Preconditions.checkNotNull(message, "DataMineRequest message cannot be null");
        Preconditions.checkNotNull(INSTANCE, "DataMineRequestProducer service is not initialized yet");
        INSTANCE.sendMessage(message);
    }

}

@Configuration
class DataMineRequestProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Bean
    public NewTopic datamineMovieTopic() {
        return new NewTopic(Topics.DATAMINE_MOVIE, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, Movie> movieProducerFactory() {
        Map<String, Object> configProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MovieSerializer.class.getName());
        Preconditions.checkNotNull(configProps, "Failed to create Kafka producer configuration for Movie");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Movie> movieKafkaTemplate() {
        ProducerFactory<String, Movie> factory = movieProducerFactory();
        Preconditions.checkNotNull(factory, "Failed to create Kafka factory for Movie");
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public NewTopic datamineSeriesTopic() {
        return new NewTopic(Topics.DATAMINE_SERIES, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, Series> seriesProducerFactory() {
        Map<String, Object> configProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SeriesSerializer.class.getName());
        Preconditions.checkNotNull(configProps, "Failed to create Kafka producer configuration for Series");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Series> seriesKafkaTemplate() {
        ProducerFactory<String, Series> factory = seriesProducerFactory();
        Preconditions.checkNotNull(factory, "Failed to create Kafka factory for Series");
        return new KafkaTemplate<>(factory);
    }
}