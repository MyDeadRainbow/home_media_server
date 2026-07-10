package com.hms.catalog.messaging;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.hms.catalog.datamine.api.MediaDbApiFactory;
import com.hms.catalog.media.MovieParser;
import com.hms.catalog.media.ParseEntry;
import com.hms.catalog.media.SeriesParser;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateDeserializer;
import com.hms.shared.messaging.catalogupdates.FilePathRecord;

@Service
public class CatalogUpdateConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogUpdateConsumer.class);

    public static final String GROUP_ID = "catalog-service";

    @KafkaListener(topics = CatalogUpdate.TOPIC, groupId = GROUP_ID, containerFactory = "catalogUpdateKafkaListenerContainerFactory", autoStartup = "true")
    public void listen(CatalogUpdate message) {
        switch (message.updateType()) {
            case CREATED -> handleMediaCreated(message);
            case UPDATED -> handleMediaUpdated(message);
            case DELETED -> handleMediaDeleted(message);
        }
    }

    private void handleMediaCreated(CatalogUpdate message) {
        switch (message.mediaType()) {
            case SERIES -> handleSeriesCreated(message);
            case MOVIE -> handleMovieCreated(message);
            default -> LOG.warn("Received media created message with unsupported media type: {}", message.mediaType());
        }
    }

    private void handleSeriesCreated(CatalogUpdate message) {
        List<Series> seriesList = SeriesParser.builder()
                .addFilePaths(message.filePaths().stream().map(r -> new ParseEntry(r.mediaId(), r.filePath())).toList())
                .build()
                .parse();
        try {
            seriesList.forEach(series -> {
                try {
                    new Series.Dao().insert(series);
                    MediaDbApiFactory.createTMDBApi().searchSeries(series)
                            .thenAccept(s -> {
                                try {
                                    new Series.Dao().update(s);
                                } catch (SQLException e) {
                                    LOG.error("Error updating series: {}", s.title(), e);
                                }
                            });
                } catch (Exception e) {
                    LOG.error("Error inserting series: {}", series.title(), e);
                }
            });
        } catch (Exception e) {
            LOG.error("Error inserting series", e);
        }
    }

    private void handleMovieCreated(CatalogUpdate message) {
        FilePathRecord filePathRecord = message.filePaths().get(0);
        Movie movie = new MovieParser(new ParseEntry(filePathRecord.mediaId(), filePathRecord.filePath())).parse();
        try {
            new Movie.Dao().insert(movie);
            MediaDbApiFactory.createTMDBApi().searchMovie(movie)
                    .thenAccept(m -> {
                        try {
                            new Movie.Dao().update(m);
                        } catch (SQLException e) {
                            LOG.error("Error updating movie: {}", m.title(), e);
                        }
                    });
        } catch (Exception e) {
            LOG.error("Error inserting movie: {}", movie.title(), e);
        }
    }

    private void handleMediaUpdated(CatalogUpdate message) {
        
    }

    private void handleMediaDeleted(CatalogUpdate message) {
        
    }
}

@Configuration
class CatalogUpdateConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Bean
    public NewTopic catalogUpdatesTopic() {
        return new NewTopic(CatalogUpdate.TOPIC, 1, (short) 1);
    }

    @Bean
    public ConsumerFactory<String, CatalogUpdate> catalogUpdateConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, CatalogUpdateConsumer.GROUP_ID,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CatalogUpdateDeserializer.class.getName());
        Preconditions.checkNotNull(props, "Kafka consumer properties cannot be null");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogUpdate> catalogUpdateKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CatalogUpdate> factory = new ConcurrentKafkaListenerContainerFactory<>();
        ConsumerFactory<String, CatalogUpdate> consumerFactory = catalogUpdateConsumerFactory();
        Preconditions.checkNotNull(consumerFactory, "Failed to create Kafka consumer factory for CatalogUpdates");
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}