package com.hms.catalog.messaging;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.hms.catalog.MediaItem;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateDeserializer;

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
        MediaItem mediaItem = new MediaItem(message.mediaId(), message.title(), message.mediaType().name(), null, null,
                null, "api/stream/files/" + message.mediaId());
        try {
            mediaItem.insert();
        } catch (Exception e) {
            LOG.error("Error inserting media item", e);
        }
    }

    private void handleMediaUpdated(CatalogUpdate message) {
        // try {
        //     MediaItem mediaItem = MediaItem.getById(message.mediaId());
        //     mediaItem.setTitle(message.title());
        //     mediaItem.setMediaType(message.mediaType().name());
        //     mediaItem.update();
        // } catch (DBFileNotFoundException e) {
        //     LOG.warn("Media item not found for update, creating new item with id: {}", message.mediaId());
        //     handleMediaCreated(message);
        // } catch (SQLException | GetConnectionException e) {
        //     LOG.error("Error updating media item", e);
        // }
    }

    private void handleMediaDeleted(CatalogUpdate message) {
        // try {
        //     MediaItem mediaItem = MediaItem.getById(message.mediaId());
        //     mediaItem.delete();
        // } catch (DBFileNotFoundException e) {
        //     LOG.warn("Media item not found for deletion, id: {}", message.mediaId());
        // } catch (SQLException | GetConnectionException e) {
        //     LOG.error("Error deleting media item", e);
        // }
    }
}

@Configuration
class CatalogUpdateConsumerConfig {
    
    @Bean
    public NewTopic catalogUpdatesTopic() {
        return new NewTopic(CatalogUpdate.TOPIC, 1, (short) 1);
    }


    @Bean
    public ConsumerFactory<String, CatalogUpdate> catalogUpdateConsumerFactory() {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
            ConsumerConfig.GROUP_ID_CONFIG, CatalogUpdateConsumer.GROUP_ID,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CatalogUpdateDeserializer.class.getName()
        );
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