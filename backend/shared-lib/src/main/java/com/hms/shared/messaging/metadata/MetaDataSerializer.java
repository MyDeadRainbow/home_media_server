package com.hms.shared.messaging.metadata;

import org.apache.kafka.common.serialization.Serializer;

public class MetaDataSerializer implements Serializer<MetaData> {

    @Override
    public byte[] serialize(String topic, MetaData data) {
        return data.toJsonObject().toString().getBytes();
    }
    
}
