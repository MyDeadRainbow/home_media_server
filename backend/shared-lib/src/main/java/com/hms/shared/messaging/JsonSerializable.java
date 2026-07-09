package com.hms.shared.messaging;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public interface JsonSerializable {

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
                    case JsonSerializable js -> json.add(field.getName(), js.toJson());
                    case null -> json.add(field.getName(), JsonNull.INSTANCE);
                    case List<?> list -> {
                        var jsonArray = new JsonArray();
                        for (var item : list) {
                            if (item instanceof JsonSerializable) {
                                jsonArray.add(((JsonSerializable) item).toJson());
                            } else {
                                jsonArray.add(item.toString());
                            }
                        }
                        json.add(field.getName(), jsonArray);
                    }
                    case byte[] byteArray -> {
                        //base64 encode the byte array and add it as a string
                        String base64Encoded = Base64.getEncoder().encodeToString(byteArray);
                        json.addProperty(field.getName(), base64Encoded);
                    }
                    case Object[] array -> {
                        var jsonArray = new JsonArray();
                        for (var item : array) {
                            if (item instanceof JsonSerializable) {
                                jsonArray.add(((JsonSerializable) item).toJson());
                            } else {
                                jsonArray.add(item.toString());
                            }
                        }
                        json.add(field.getName(), jsonArray);
                    }
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

    public static <T extends JsonSerializable> T fromJson(String json, Class<T> clazz) throws DeserializeJsonException {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return fromJsonObject(jsonObject, clazz);
    }

    public static <T extends JsonSerializable> T fromJsonObject(JsonObject json, Class<T> clazz) throws DeserializeJsonException {
        // use reflection to find a constructor that matches the fields in the JSON
        var constructors = clazz.getConstructors();
        for (var constructor : constructors) {
            var params = constructor.getParameters();
            Object[] args = new Object[params.length];
            boolean matches = true;
            for (int i = 0; i < params.length; i++) {
                var param = params[i];
                String paramName = param.getName();
                if (json.has(paramName)) {
                    switch (param.getType()) {
                        case Class<?> c when c == String.class && json.get(paramName).isJsonPrimitive() ->
                            args[i] = json.get(paramName).getAsString();

                        case Class<?> c when (c == Integer.class || c == int.class)
                                && json.get(paramName).isJsonPrimitive() ->
                            args[i] = json.get(paramName).getAsInt();

                        case Class<?> c when (c == Long.class || c == long.class)
                                && json.get(paramName).isJsonPrimitive() ->
                            args[i] = json.get(paramName).getAsLong();

                        case Class<?> c when (c == Float.class || c == float.class)
                                && json.get(paramName).isJsonPrimitive() ->
                            args[i] = json.get(paramName).getAsFloat();

                        case Class<?> c when (c == Double.class || c == double.class)
                                && json.get(paramName).isJsonPrimitive() ->
                            args[i] = json.get(paramName).getAsDouble();

                        case Class<?> c when (c == Boolean.class || c == boolean.class)
                                && json.get(paramName).isJsonPrimitive() ->
                            args[i] = json.get(paramName).getAsBoolean();

                        case Class<?> c when c == LocalDate.class && json.get(paramName).isJsonPrimitive() ->
                            args[i] = LocalDate.parse(json.get(paramName).getAsString());

                        case Class<?> c when c == LocalDateTime.class && json.get(paramName).isJsonPrimitive() ->
                            args[i] = LocalDateTime.parse(json.get(paramName).getAsString());

                        case Class<?> c when c == Date.class && json.get(paramName).isJsonPrimitive() ->
                            args[i] = Date.from(LocalDateTime.parse(json.get(paramName).getAsString())
                                    .atZone(ZoneId.systemDefault()).toInstant());

                        case Class<?> c when c.isEnum() && json.get(paramName).isJsonPrimitive() -> {
                            @SuppressWarnings({ "unchecked", "rawtypes" })
                            Object enumValue = Enum.valueOf((Class<Enum>) c,
                                    json.get(paramName).getAsString());
                            args[i] = enumValue;
                        }

                        case Class<?> c when c == JsonObject.class && json.get(paramName).isJsonObject() -> {
                            args[i] = json.get(paramName).getAsJsonObject();
                        }

                        case Class<?> c when JsonSerializable.class.isAssignableFrom(c)
                                && json.get(paramName).isJsonObject() -> {
                            @SuppressWarnings("unchecked")
                            Object nestedObject = fromJsonObject(json.get(paramName).getAsJsonObject(),
                                    (Class<? extends JsonSerializable>) c);
                            args[i] = nestedObject;
                        }

                        case Class<?> _ when json.get(paramName).isJsonNull() -> {
                            args[i] = null;
                        }

                        case Class<?> c when List.class.isAssignableFrom(c) && json.get(paramName).isJsonArray() -> {
                            // Handle List deserialization
                            var jsonArray = json.get(paramName).getAsJsonArray();
                            var listType = param.getParameterizedType();
                            if (listType instanceof ParameterizedType pt) {
                                var itemType = (Class<?>) pt.getActualTypeArguments()[0];
                                var list = new java.util.ArrayList<>();
                                for (var item : jsonArray) {
                                    if (JsonSerializable.class.isAssignableFrom(itemType)
                                            && item.isJsonObject()) {
                                        @SuppressWarnings("unchecked")
                                        Object nestedItem = fromJsonObject(item.getAsJsonObject(),
                                                (Class<? extends JsonSerializable>) itemType);
                                        list.add(nestedItem);
                                    } else {
                                        // Handle primitive types or other types as needed
                                        list.add(item.toString());
                                    }
                                }
                                args[i] = list;
                            } else {
                                throw new DeserializeJsonException(
                                        "Failed to deserialize JSON: List type information is missing");
                            }
                        }

                        case Class<?> c when c == byte[].class && json.get(paramName).isJsonPrimitive() -> {
                            // Handle byte[] deserialization from base64 encoded string
                            String base64Encoded = json.get(paramName).getAsString();
                            args[i] = Base64.getDecoder().decode(base64Encoded);
                        }

                        case Class<?> c when c.isArray() && json.get(paramName).isJsonArray() -> {
                            // Handle Array deserialization
                            var jsonArray = json.get(paramName).getAsJsonArray();
                            var componentType = c.getComponentType();
                            var array = Array.newInstance(componentType, jsonArray.size());
                            for (int j = 0; j < jsonArray.size(); j++) {
                                var item = jsonArray.get(j);
                                if (JsonSerializable.class.isAssignableFrom(componentType)
                                        && item.isJsonObject()) {
                                    @SuppressWarnings("unchecked")
                                    Object nestedItem = fromJsonObject(item.getAsJsonObject(),
                                            (Class<? extends JsonSerializable>) componentType);
                                    Array.set(array, j, nestedItem);
                                } else {
                                    // Handle primitive types or other types as needed
                                    Array.set(array, j, item.toString());
                                }
                            }
                            args[i] = array;
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
