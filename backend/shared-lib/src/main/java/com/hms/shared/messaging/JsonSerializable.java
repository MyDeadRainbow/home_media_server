package com.hms.shared.messaging;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public interface JsonSerializable<T> {

    public default JsonObject toJson() throws SerializeJsonException {
        JsonObject json = new JsonObject();
        for (var field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(this);
                switch (value) {
                    case String s -> json.addProperty(field.getName(), s);
                    case Integer i -> json.addProperty(field.getName(), i);
                    case Long l -> json.addProperty(field.getName(), l);
                    case Float f -> json.addProperty(field.getName(), f);
                    case Double d -> json.addProperty(field.getName(), d);
                    case Boolean b -> json.addProperty(field.getName(), b);
                    case Enum<?> e -> json.addProperty(field.getName(), e.name());
                    case JsonSerializable<?> js -> json.add(field.getName(), js.toJson());
                    case null -> json.add(field.getName(), JsonNull.INSTANCE);
                    default -> json.addProperty(field.getName(), value.toString());
                }
                // if (value instanceof JsonSerializable) {
                // json.add(field.getName(), ((JsonSerializable<?>) value).toJson());
                // } else {
                // json.addProperty(field.getName(), value.toString());
                // }
            } catch (IllegalAccessException e) {
                throw new SerializeJsonException("Failed to serialize JSON", e);
            }
        }
        return json;
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws DeserializeJsonException {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return fromJsonObject(jsonObject, clazz);
    }

    public static <T> T fromJsonObject(JsonObject json, Class<T> clazz) throws DeserializeJsonException {
        // use reflection to find a constructor that matches the fields in the JSON
        var constructors = clazz.getConstructors();
        for (var constructor : constructors) {
            var params = constructor.getParameters();
            Object[] args = new Object[params.length];
            boolean matches = true;
            for (int i = 0; i < params.length; i++) {
                var param = params[i];
                if (json.has(param.getName())) {
                    switch (param.getType()) {
                        case Class<?> c when c == String.class ->
                            args[i] = json.get(param.getName()).getAsString();

                        case Class<?> c when c == Integer.class || c == int.class ->
                            args[i] = json.get(param.getName()).getAsInt();

                        case Class<?> c when c == Long.class || c == long.class ->
                            args[i] = json.get(param.getName()).getAsLong();

                        case Class<?> c when c == Float.class || c == float.class ->
                            args[i] = json.get(param.getName()).getAsFloat();

                        case Class<?> c when c == Double.class || c == double.class ->
                            args[i] = json.get(param.getName()).getAsDouble();

                        case Class<?> c when c == Boolean.class || c == boolean.class ->
                            args[i] = json.get(param.getName()).getAsBoolean();

                        case Class<?> c when c == LocalDate.class ->
                            args[i] = LocalDate.parse(json.get(param.getName()).getAsString());

                        case Class<?> c when c == LocalDateTime.class ->
                            args[i] = LocalDateTime.parse(json.get(param.getName()).getAsString());

                        case Class<?> c when c == Date.class ->
                            args[i] = Date.from(LocalDateTime.parse(json.get(param.getName()).getAsString())
                                    .atZone(ZoneId.systemDefault()).toInstant());

                        case Class<?> c when c.isEnum() -> {
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            Object enumValue = Enum.valueOf((Class<Enum>) c, json.get(param.getName()).getAsString());
                            args[i] = enumValue;
                        }
                        default -> {
                            matches = false;
                            break;
                        }
                    }
                } else {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                try {
                    @SuppressWarnings("unchecked")
                    T instance = (T) constructor.newInstance(args);
                    return instance;
                } catch (Exception e) {
                    throw new DeserializeJsonException("Failed to deserialize JSON", e);
                }
            } else {
                throw new DeserializeJsonException("No matching constructor found for class: " + clazz.getName());
            }
        }
        return null;
    }
}
