package com.hms.shared.messaging.datamining;

import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.messaging.JsonSerializable;

public class DataMineRequestDeserializer implements Deserializer<DataMineRequest> {

    public DataMineRequest deserialize(JsonObject jsonObject) {
        DataMineRequest request = null;
        request = JsonSerializable.fromJsonObject(jsonObject, DataMineRequest.Episode.class);
        if (request != null) {
            return request;
        }
        request = JsonSerializable.fromJsonObject(jsonObject, DataMineRequest.Movie.class);
        if (request != null) {
            return request;
        }
        request = JsonSerializable.fromJsonObject(jsonObject, DataMineRequest.Series.class);
        return request;
    }

    @Override
    public DataMineRequest deserialize(String topic, byte[] data) {
        com.google.gson.JsonObject jsonObject = JsonParser.parseString(new String(data)).getAsJsonObject();
        return deserialize(jsonObject);
    }
    
}
