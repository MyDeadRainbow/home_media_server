package com.hms.shared.messaging.metadata;

import org.apache.kafka.common.serialization.Serializer;

import com.hms.shared.messaging.SerializeJsonException;

public class MetaDataSerializer implements Serializer<MetaData> {

    @Override
    public byte[] serialize(String topic, MetaData data) {
        try {
            return data.toJson().toString().getBytes();
        } catch (SerializeJsonException e) {
            throw new RuntimeException("Failed to serialize MetaData to JSON", e);
        }
    }
    
}
