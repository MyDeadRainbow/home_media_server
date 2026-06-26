package com.hms.shared.messaging.mediaupdates;

import org.apache.kafka.common.serialization.Serializer;

import com.hms.shared.messaging.SerializeJsonException;

public class MediaUpdateSerializer implements Serializer<MediaUpdate> {

    @Override
    public byte[] serialize(String topic, MediaUpdate data) {
        try {
            return data.toJson().toString().getBytes();
        } catch (SerializeJsonException e) {
            throw new RuntimeException("Failed to serialize MediaUpdate to JSON", e);
        }
    }
    
}
