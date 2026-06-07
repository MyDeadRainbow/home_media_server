package com.hms.shared.messaging.mediaupdates;

import java.io.UnsupportedEncodingException;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MediaUpdateDeserializer implements Deserializer<MediaUpdate> {

    @Override
    public MediaUpdate deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        } else {
            String str = new String(data);
            JsonObject json = JsonParser.parseString(str).getAsJsonObject();
            MediaUpdateType type = MediaUpdateType.valueOf(json.get("type").getAsString());
            String id = json.get("id").getAsString();
            return new MediaUpdate(type, id);
        }
    }

}
