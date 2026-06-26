package com.hms.acquisition.datamine;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import com.google.common.base.Preconditions;
import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.datamining.DataMineRequestDeserializer;

public class DataMineRequestConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(DataMineRequestConsumer.class);

    public static final String GROUP_ID = "acquisition-service";

    @KafkaListener(topics = DataMineRequest.TOPIC, groupId = GROUP_ID, containerFactory = "dataMineRequestKafkaListenerContainerFactory", autoStartup = "true")
    public void listen(DataMineRequest message) {
        LOG.info("Received DataMineRequest message: {}", message);
        switch (message) {
            case DataMineRequest.Episode episode -> handleEpisodeRequest(episode);
            case DataMineRequest.Movie movie -> handleMovieRequest(movie);
            case DataMineRequest.Series series -> handleSeriesRequest(series);
            default -> LOG.warn("Received DataMineRequest message with unsupported type: {}", message.getClass().getName());
        }
    }

    private void handleEpisodeRequest(DataMineRequest.Episode episode) {
        LOG.info("Handling DataMineRequest for Episode: {}", episode);
        try {
            DatamineEpisodeHandler episodeHandler = new DatamineEpisodeHandler();
            episodeHandler.handle(episode);
        } catch (Exception e) {
            LOG.error("Error handling DataMineRequest for Episode: {}", episode, e);
        }
    }

    private void handleMovieRequest(DataMineRequest.Movie movie) {
        LOG.info("Handling DataMineRequest for Movie: {}", movie);
        try {
            DatamineMovieHandler movieHandler = new DatamineMovieHandler();
            movieHandler.handle(movie);
        } catch (Exception e) {
            LOG.error("Error handling DataMineRequest for Movie: {}", movie, e);
        }
    }

    private void handleSeriesRequest(DataMineRequest.Series series) {
        LOG.info("Handling DataMineRequest for Series: {}", series);
        try {
            DatamineSeriesHandler seriesHandler = new DatamineSeriesHandler();
            seriesHandler.handle(series);
        } catch (Exception e) {
            LOG.error("Error handling DataMineRequest for Series: {}", series, e);
        }
    }
}


@Configuration
class DataMineRequestConsumerConfig {

    @Bean
    public NewTopic dataMineRequestsTopic() {
        return new NewTopic(DataMineRequest.TOPIC, 1, (short) 1);
    }

    @Bean
    public ConsumerFactory<String, DataMineRequest> dataMineRequestConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ConsumerConfig.GROUP_ID_CONFIG, DataMineRequestConsumer.GROUP_ID,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, DataMineRequestDeserializer.class.getName());
        Preconditions.checkNotNull(props, "Kafka consumer properties cannot be null");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DataMineRequest> dataMineRequestKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DataMineRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        ConsumerFactory<String, DataMineRequest> consumerFactory = dataMineRequestConsumerFactory();
        Preconditions.checkNotNull(consumerFactory, "Failed to create Kafka consumer factory for DataMineRequests");
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}