package com.hms.shared.messaging.datamining;

import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.messaging.DeserializeJsonException;
import com.hms.shared.messaging.JsonSerializable;

public class DataMineRequestDeserializer implements Deserializer<DataMineRequest> {

    public DataMineRequest deserialize(JsonObject jsonObject) {
        DataMineRequest request = null;
        // try {
        //     request = JsonSerializable.fromJsonObject(jsonObject, DataMineRequest.Episode.class);
        // } catch (DeserializeJsonException e) {
        //     // suppress exception and try next type
        // }
        // if (request != null) {
        //     return request;
        // }
        try {
            request = JsonSerializable.fromJsonObject(jsonObject, DataMineRequest.Movie.class);
        } catch (DeserializeJsonException e) {
            // suppress exception and try next type
        }
        if (request != null) {
            return request;
        }
        try {
            request = JsonSerializable.fromJsonObject(jsonObject, DataMineRequest.Series.class);
        } catch (DeserializeJsonException e) {
            // suppress exception and try next type
        }
        return request;
    }

    @Override
    public DataMineRequest deserialize(String topic, byte[] data) {
        com.google.gson.JsonObject jsonObject = JsonParser.parseString(new String(data)).getAsJsonObject();
        return deserialize(jsonObject);
    }

}
