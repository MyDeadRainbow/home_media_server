package com.hms.shared.media.serialize;

import org.apache.kafka.common.serialization.Serializer;

import com.hms.shared.media.Movie;
import com.hms.shared.messaging.SerializeJsonException;

public class MovieSerializer implements Serializer<Movie> {

    @Override
    public byte[] serialize(String topic, Movie data) {
        try {
            return data.toJson().toString().getBytes();
        } catch (SerializeJsonException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
}
