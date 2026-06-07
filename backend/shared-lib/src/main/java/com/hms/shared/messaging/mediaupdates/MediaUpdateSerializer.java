package com.hms.shared.messaging.mediaupdates;

import org.apache.kafka.common.serialization.Serializer;

import com.google.gson.JsonObject;

public class MediaUpdateSerializer implements Serializer<MediaUpdate> {

    @Override
    public byte[] serialize(String topic, MediaUpdate data) {
        JsonObject json = data.toJsonObject();
        return json.toString().getBytes();
    }
    
}
