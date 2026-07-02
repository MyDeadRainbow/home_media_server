package com.hms.shared.media.serialize;

import org.apache.kafka.common.serialization.Deserializer;

import com.hms.shared.media.Movie;
import com.hms.shared.messaging.DeserializeJsonException;
import com.hms.shared.messaging.JsonSerializable;

public class MovieDeserializer implements Deserializer<Movie> {

    @Override
    public Movie deserialize(String topic, byte[] data) {
        String jsonString = new String(data);
        try {
            return JsonSerializable.fromJson(jsonString, Movie.class);
        } catch (DeserializeJsonException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
