package com.hms.shared.messaging.catalogupdates;

import org.apache.kafka.common.serialization.Serializer;

import com.hms.shared.messaging.SerializeJsonException;

public class CatalogUpdateSerializer implements Serializer<CatalogUpdate> {

    @Override
    public byte[] serialize(String topic, CatalogUpdate data) {
        try {
            return data.toJson().toString().getBytes();
        } catch (SerializeJsonException e) {
            throw new RuntimeException("Failed to serialize CatalogUpdate to JSON", e);
        }
    }
    
}
