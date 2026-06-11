package com.hms.shared.messaging.catalogupdates;

import org.apache.kafka.common.serialization.Deserializer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hms.shared.media.MediaCategory;

public class CatalogUpdateDeserializer implements Deserializer<CatalogUpdate> {
    @Override
    public CatalogUpdate deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        } else {
            String str = new String(data);
            JsonObject json = JsonParser.parseString(str).getAsJsonObject();
            CatalogUpdateType updateType = CatalogUpdateType.valueOf(json.get("updateType").getAsString());
            String mediaId = json.get("mediaId").getAsString();
            String title = json.get("title").getAsString();
            MediaCategory mediaType = MediaCategory.valueOf(json.get("mediaType").getAsString());
            Integer year = json.has("year") && !json.get("year").isJsonNull()
                    ? Integer.valueOf(json.get("year").getAsString())
                    : null;
            String description = json.has("description") && !json.get("description").isJsonNull()
                    ? json.get("description").getAsString()
                    : null;
            CatalogUpdate update = new CatalogUpdate(mediaId, updateType, title, mediaType, year, description);
            return update;
        }
    }
}
