package com.hms.shared.messaging;

import com.google.gson.JsonObject;

public interface JsonSerializable<T> {
    public default JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        for (var field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(this);
                if (value instanceof JsonSerializable) {
                    json.add(field.getName(), ((JsonSerializable<?>) value).toJsonObject());
                } else {
                    json.addProperty(field.getName(), value.toString());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    public T fromJsonObject(JsonObject json);
}
