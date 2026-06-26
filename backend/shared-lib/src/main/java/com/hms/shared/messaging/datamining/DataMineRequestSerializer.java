package com.hms.shared.messaging.datamining;

import org.apache.kafka.common.serialization.Serializer;

import com.hms.shared.messaging.SerializeJsonException;

public class DataMineRequestSerializer implements Serializer<DataMineRequest> {

    @Override
    public byte[] serialize(String topic, DataMineRequest data) {
        try {
            return data.toJson().toString().getBytes();
        } catch (SerializeJsonException e) {
            throw new RuntimeException("Failed to serialize DataMineRequest to JSON", e);
        }
    }

}
