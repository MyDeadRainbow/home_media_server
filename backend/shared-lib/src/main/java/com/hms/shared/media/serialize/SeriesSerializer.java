package com.hms.shared.media.serialize;

import org.apache.kafka.common.serialization.Serializer;

import com.hms.shared.media.Series;
import com.hms.shared.messaging.SerializeJsonException;

public class SeriesSerializer implements Serializer<Series> {

    @Override
    public byte[] serialize(String topic, Series data) {
        try {
            return data.toJson().toString().getBytes();
        } catch (SerializeJsonException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
}
