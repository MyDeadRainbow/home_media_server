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

    public static <T> T fromJsonObject(JsonObject json, Class<T> clazz) {
        // use reflection to find a constructor that matches the fields in the JSON object
        try {
            var constructors = clazz.getConstructors();
            for (var constructor : constructors) {
                var params = constructor.getParameters();
                Object[] args = new Object[params.length];
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    var param = params[i];
                    if (json.has(param.getName())) {
                        args[i] = json.get(param.getName()).getAsString();
                    } else {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return (T) constructor.newInstance(args);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
