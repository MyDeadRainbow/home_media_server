package com.hms.shared.messaging.datamining;

import org.apache.kafka.common.serialization.Serializer;

public class DataMineRequestSerializer implements Serializer<DataMineRequest> {

    @Override
    public byte[] serialize(String topic, DataMineRequest data) {
        return data.toJsonObject().toString().getBytes();
    }

}
