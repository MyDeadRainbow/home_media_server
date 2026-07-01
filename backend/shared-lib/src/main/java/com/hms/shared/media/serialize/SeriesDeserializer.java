package com.hms.shared.media.serialize;

import org.apache.kafka.common.serialization.Deserializer;

import com.hms.annotated.sql.media.Series;
import com.hms.shared.messaging.DeserializeJsonException;
import com.hms.shared.messaging.JsonSerializable;

public class SeriesDeserializer implements Deserializer<Series> {

    @Override
    public Series deserialize(String topic, byte[] data) {
        String jsonString = new String(data);
        try {
            return JsonSerializable.fromJson(jsonString, Series.class);
        } catch (DeserializeJsonException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
}
