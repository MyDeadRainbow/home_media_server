package com.hms.shared.messaging.metadata;

import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.messaging.DeserializeJsonException;
import com.hms.shared.messaging.JsonSerializable;

// public class MetaDataDeserializer implements Deserializer<MetaData> {

//     @Override
//     public MetaData deserialize(String topic, byte[] data) {
//         String jsonString = new String(data);
//         JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

//         MetaData metaData = null;        
//         try {
//             metaData = JsonSerializable.fromJsonObject(jsonObject, MetaData.Episode.class);
//         } catch (DeserializeJsonException e) {
//             // suppress exception and try next type
//         }

//         if (metaData == null) {
//             try {
//                 metaData = JsonSerializable.fromJsonObject(jsonObject, MetaData.Movie.class);
//             } catch (DeserializeJsonException e) {
//                 // suppress exception and try next type
//             }
//         }

//         if (metaData == null) {
//             try {
//                 metaData = JsonSerializable.fromJsonObject(jsonObject, MetaData.Series.class);
//             } catch (DeserializeJsonException e) {
//                 // suppress exception and try next type
//             }
//         }
//         return metaData;
//     }

// }
