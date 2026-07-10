package com.hms.shared.messaging.catalogupdates;

import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonParser;
import com.hms.shared.messaging.JsonSerializable;

public class CatalogUpdateDeserializer implements Deserializer<CatalogUpdate> {
    @Override
    public CatalogUpdate deserialize(String topic, byte[] data) {
        try {
            return JsonSerializable.fromJsonObject(JsonParser.parseString(new String(data)).getAsJsonObject(), CatalogUpdate.class);
        } catch (Exception e) {
            return null;
        }
    }
}
