package com.hms.shared.messaging.mediaupdates;

import com.google.gson.JsonObject;
import com.hms.shared.messaging.JsonSerializable;

public record MediaUpdate(MediaUpdateType type, String id) implements JsonSerializable<MediaUpdate> {

    @Override
    public MediaUpdate fromJsonObject(JsonObject json) {
        MediaUpdateType type = MediaUpdateType.valueOf(json.get("type").getAsString());
        String id = json.get("id").getAsString();
        return new MediaUpdate(type, id);
    }
}

enum MediaUpdateType {
    CREATED,
    UPDATED,
    DELETED
}
