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
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;
import com.hms.shared.media.serialize.MovieDeserializer;
import com.hms.shared.media.serialize.SeriesDeserializer;
import com.hms.shared.messaging.Topics;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
// import com.hms.shared.messaging.metadata.MetaData;
// import com.hms.shared.messaging.metadata.MetaDataDeserializer;

@Service
public class MetaDataConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataConsumer.class);

    public static final String GROUP_ID = "catalog-service";

    @KafkaListener(topics = Topics.METADATA_MOVIE, groupId = GROUP_ID, containerFactory = "movieKafkaListenerContainerFactory", autoStartup = "true")
    public void listenMovie(Movie message) {
        // switch (message) {
        //     case MetaData.Episode episode -> handleEpisodeMetaData(episode);
        //     case MetaData.Movie movie -> handleMovieMetaData(movie);
        //     case MetaData.Series series -> handleSeriesMetaData(series);
        //     default ->
        //         LOG.warn("Received metadata message with unsupported type: {}", message.getClass().getSimpleName());
        // }
    }

    @KafkaListener(topics = Topics.METADATA_SERIES, groupId = GROUP_ID, containerFactory = "seriesKafkaListenerContainerFactory", autoStartup = "true")
    public void listenSeries(Series message) {
        // switch (message) {
        //     case MetaData.Episode episode -> handleEpisodeMetaData(episode);
        //     case MetaData.Movie movie -> handleMovieMetaData(movie);
        //     case MetaData.Series series -> handleSeriesMetaData(series);
        //     default ->
        //         LOG.warn("Received metadata message with unsupported type: {}", message.getClass().getSimpleName());
        // }
    }

    // private void handleEpisodeMetaData(MetaData.Episode message) {
    //     LOG.info("Received Episode MetaData: {}", message);
    //     try {
    //         Episode episode = new Episode.Dao().get(message.episodeId());
    //         com.hms.shared.media.metadata.MetaData currentMetaData = episode.metaData();

    //         com.hms.shared.media.metadata.MetaData updatedMetaData = new com.hms.shared.media.metadata.MetaData(
    //                 currentMetaData.metaDataId(),
    //                 message.plotSummary(),
    //                 message.airDate(),
    //                 message.rating());

    //         episode = episode.withMetaData(updatedMetaData);
    //         new Episode.Dao().update(episode);
    //     } catch (SQLException e) {
    //         LOG.error("Failed to retrieve episode from database: {}", e.getMessage(), e);
    //     }
    // }

    // private void handleMovieMetaData(MetaData.Movie message) {
    //     LOG.info("Received Movie MetaData: {}", message);
    //     try {
    //         Movie movie = new Movie.Dao().get(message.movieId());
    //         com.hms.shared.media.metadata.MetaData currentMetaData = movie.metaData();

    //         com.hms.shared.media.metadata.MetaData updatedMetaData = new com.hms.shared.media.metadata.MetaData(
    //                 currentMetaData.metaDataId(),
    //                 message.plotSummary(),
    //                 message.releaseDate(),
    //                 message.rating());

    //         movie = movie.withMetaData(updatedMetaData);
    //         new Movie.Dao().update(movie);
    //     } catch (Exception e) {
    //         LOG.error("Failed to handle Movie metadata: {}", e.getMessage(), e);
    //     }
    // }

    // private void handleSeriesMetaData(MetaData.Series message) {
    //     LOG.info("Received Series MetaData: {}", message);
    //     try {
    //         Series series = new Series.Dao().get(message.seriesId());
    //         com.hms.shared.media.metadata.MetaData currentMetaData = series.metaData();

    //         com.hms.shared.media.metadata.MetaData updatedMetaData = new com.hms.shared.media.metadata.MetaData(
    //                 currentMetaData.metaDataId(),
    //                 message.plotSummary(),
    //                 message.firstAirDate(),
    //                 message.rating());

    //         series = series.withMetaData(updatedMetaData);
    //         new Series.Dao().update(series);

    //         for (MetaData.Season season : message.seasons()) {
    //             for (MetaData.Episode episode : season.episodes()) {
    //                 Episode episodeEntity = new Episode.Dao().get(episode.episodeId());
    //                 com.hms.shared.media.metadata.MetaData episodeCurrentMetaData = episodeEntity.metaData();
    //                 com.hms.shared.media.metadata.MetaData updatedEpisodeMetaData = new com.hms.shared.media.metadata.MetaData(
    //                         episodeCurrentMetaData.metaDataId(),
    //                         episode.plotSummary(),
    //                         episode.airDate(),
    //                         episode.rating());
    //                 episodeEntity = episodeEntity.withMetaData(updatedEpisodeMetaData);
    //                 new Episode.Dao().update(episodeEntity);
    //             }
    //         }
    //     } catch (Exception e) {
    //         LOG.error("Failed to handle Series metadata: {}", e.getMessage(), e);
    //     }
    // }
}

@Configuration
class MetaDataConsumerConfig {

    @Bean
    public NewTopic metaDataMovieTopic() {
        return new NewTopic(Topics.METADATA_MOVIE, 1, (short) 1);
    }

    @Bean
    public ConsumerFactory<String, Movie> movieConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ConsumerConfig.GROUP_ID_CONFIG, MetaDataConsumer.GROUP_ID,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MovieDeserializer.class.getName());
        Preconditions.checkNotNull(props, "Kafka consumer properties cannot be null");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Movie> movieKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Movie> factory = new ConcurrentKafkaListenerContainerFactory<>();
        ConsumerFactory<String, Movie> consumerFactory = movieConsumerFactory();
        Preconditions.checkNotNull(consumerFactory, "Failed to create Kafka consumer factory for Movie");
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public NewTopic metaDataSeriesTopic() {
        return new NewTopic(Topics.METADATA_SERIES, 1, (short) 1);
    }

    @Bean
    public ConsumerFactory<String, Series> seriesConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ConsumerConfig.GROUP_ID_CONFIG, MetaDataConsumer.GROUP_ID,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SeriesDeserializer.class.getName());
        Preconditions.checkNotNull(props, "Kafka consumer properties cannot be null");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Series> seriesKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Series> factory = new ConcurrentKafkaListenerContainerFactory<>();
        ConsumerFactory<String, Series> consumerFactory = seriesConsumerFactory();
        Preconditions.checkNotNull(consumerFactory, "Failed to create Kafka consumer factory for Series");
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
