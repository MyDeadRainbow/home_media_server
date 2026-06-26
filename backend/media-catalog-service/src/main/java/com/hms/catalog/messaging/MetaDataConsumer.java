package com.hms.catalog.messaging;

import java.sql.SQLException;
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
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.hms.catalog.media.Episode;
import com.hms.catalog.media.Movie;
import com.hms.catalog.media.Series;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.metadata.MetaData;
import com.hms.shared.messaging.metadata.MetaDataDeserializer;

@Service
public class MetaDataConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataConsumer.class);

    public static final String GROUP_ID = "catalog-service";

    @KafkaListener(topics = MetaData.TOPIC, groupId = GROUP_ID, containerFactory = "metaDataKafkaListenerContainerFactory", autoStartup = "true")
    public void listen(MetaData message) {
        switch (message) {
            case MetaData.Episode episode -> handleEpisodeMetaData(episode);
            case MetaData.Movie movie -> handleMovieMetaData(movie);
            case MetaData.Series series -> handleSeriesMetaData(series);
            default ->
                LOG.warn("Received metadata message with unsupported type: {}", message.getClass().getSimpleName());
        }
    }

    private void handleEpisodeMetaData(MetaData.Episode message) {
        LOG.info("Received Episode MetaData: {}", message);
        try {
            Episode episode = new Episode.Dao().get(message.episodeId());
            com.hms.catalog.media.MetaData currentMetaData = episode.metaData();

            com.hms.catalog.media.MetaData updatedMetaData = new com.hms.catalog.media.MetaData(
                    currentMetaData.metaDataId(),
                    message.plotSummary(),
                    message.airDate(),
                    message.rating());

            episode = episode.withMetaData(updatedMetaData);
            new Episode.Dao().update(episode);
        } catch (SQLException e) {
            LOG.error("Failed to retrieve episode from database: {}", e.getMessage(), e);
        }
    }

    private void handleMovieMetaData(MetaData.Movie message) {
        LOG.info("Received Movie MetaData: {}", message);
        try {
            Movie movie = new Movie.Dao().get(message.movieId());
            com.hms.catalog.media.MetaData currentMetaData = movie.metaData();

            com.hms.catalog.media.MetaData updatedMetaData = new com.hms.catalog.media.MetaData(
                    currentMetaData.metaDataId(),
                    message.plotSummary(),
                    message.releaseDate(),
                    message.rating());

            movie = movie.withMetaData(updatedMetaData);
            new Movie.Dao().update(movie);
        } catch (Exception e) {
            LOG.error("Failed to handle Movie metadata: {}", e.getMessage(), e);
        }
    }

    private void handleSeriesMetaData(MetaData.Series message) {
        LOG.info("Received Series MetaData: {}", message);
        try {
            Series series = new Series.Dao().get(message.seriesId());
            com.hms.catalog.media.MetaData currentMetaData = series.metaData();

            com.hms.catalog.media.MetaData updatedMetaData = new com.hms.catalog.media.MetaData(
                    currentMetaData.metaDataId(),
                    message.plotSummary(),
                    message.firstAirDate(),
                    message.rating());

            series = series.withMetaData(updatedMetaData);
            new Series.Dao().update(series);
        } catch (Exception e) {
            LOG.error("Failed to handle Series metadata: {}", e.getMessage(), e);
        }
    }
}

@Configuration
class MetaDataConsumerConfig {

    @Bean
    public NewTopic metaDataTopic() {
        return new NewTopic(MetaData.TOPIC, 1, (short) 1);
    }

    @Bean
    public ConsumerFactory<String, MetaData> metaDataConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ConsumerConfig.GROUP_ID_CONFIG, MetaDataConsumer.GROUP_ID,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MetaDataDeserializer.class.getName());
        Preconditions.checkNotNull(props, "Kafka consumer properties cannot be null");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MetaData> metaDataKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MetaData> factory = new ConcurrentKafkaListenerContainerFactory<>();
        ConsumerFactory<String, MetaData> consumerFactory = metaDataConsumerFactory();
        Preconditions.checkNotNull(consumerFactory, "Failed to create Kafka consumer factory for MetaData");
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
