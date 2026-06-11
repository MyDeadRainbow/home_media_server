package com.hms.shared.messaging.catalogupdates;

import org.apache.kafka.common.serialization.Serializer;

import com.google.gson.JsonObject;

public class CatalogUpdateSerializer implements Serializer<CatalogUpdate> {

    @Override
    public byte[] serialize(String topic, CatalogUpdate data) {
        JsonObject json = data.toJsonObject();
        return json.toString().getBytes();
    }
    
}
