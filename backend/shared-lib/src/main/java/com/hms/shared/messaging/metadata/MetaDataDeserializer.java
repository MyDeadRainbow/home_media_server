package com.hms.shared.messaging.metadata;

import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.messaging.JsonSerializable;

public class MetaDataDeserializer implements Deserializer<MetaData> {

    @Override
    public MetaData deserialize(String topic, byte[] data) {
        String jsonString = new String(data);
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        
        MetaData metaData = null;        
        metaData = JsonSerializable.fromJsonObject(jsonObject, MetaData.Episode.class);

        if (metaData == null) {
            metaData = JsonSerializable.fromJsonObject(jsonObject, MetaData.Movie.class);
        }

        if (metaData == null) {
            metaData = JsonSerializable.fromJsonObject(jsonObject, MetaData.Series.class);
        }
        return metaData;
    }

}
