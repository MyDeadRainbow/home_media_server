package com.hms.shared.messaging.mediaupdates;

import com.google.gson.JsonObject;
import com.hms.shared.messaging.JsonSerializable;

public record MediaUpdate(MediaUpdateType type, String id) implements JsonSerializable<MediaUpdate> {

    public static MediaUpdate created(String id) {
        return new MediaUpdate(MediaUpdateType.CREATED, id);
    }

    public static MediaUpdate updated(String id) {
        return new MediaUpdate(MediaUpdateType.UPDATED, id);
    }

    public static MediaUpdate deleted(String id) {
        return new MediaUpdate(MediaUpdateType.DELETED, id);
    }
}