package com.hms.shared.dao;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLiteMap<T extends SQLiteSerializable> implements Map<String, T> {

    Class<T> type;

    public SQLiteMap(Class<T> type) {
        this.type = type;
    }

    @Override
    public int size() {
        int size = 0;
        try {
            size = SQLiteSerializable.select(type, Map.of()).size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            return SQLiteSerializable.select(type, Map.of(SQLiteSerializable.getPrimaryKeyField(type), key)).size() > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (!type.isInstance(value)) {
            return false;
        }
        T tValue = type.cast(value);
        try {
            return SQLiteSerializable.select(type,
                    Map.of(SQLiteSerializable.getPrimaryKeyField(type), SQLiteSerializable.getPrimaryKeyValue(tValue)))
                    .size() > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T get(Object key) {
        try {
            var results = SQLiteSerializable.select(type, Map.of(SQLiteSerializable.getPrimaryKeyField(type), key));
            if (results.size() > 0) {
                return results.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T put(String key, T value) {
        try {
            // Check if an entry with the same primary key already exists
            var existing = SQLiteSerializable.select(type,
                    Map.of(SQLiteSerializable.getPrimaryKeyField(type), key));
            if (existing.size() > 0) {
                // Update existing entry
                value.update();
            } else {
                // Insert new entry
                value.insert();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    @Override
    public T remove(Object key) {
        try {
            var existing = SQLiteSerializable.select(type,
                    Map.of(SQLiteSerializable.getPrimaryKeyField(type), key));
            if (existing.size() > 0) {
                T toRemove = existing.get(0);
                toRemove.delete();
                return toRemove;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> m) {
        for (Map.Entry<? extends String, ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        try {
            var allEntries = SQLiteSerializable.select(type, Map.of());
            for (T entry : allEntries) {
                entry.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> keySet() {
        List<T> allEntries;
        try {
            allEntries = SQLiteSerializable.select(type, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return allEntries.stream()
                .map(entry -> {
                    try {
                        return SQLiteSerializable.getPrimaryKeyValue(entry).toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Collection<T> values() {
        try {
            return SQLiteSerializable.select(type, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Entry<String, T>> entrySet() {
        List<T> allEntries;
        try {
            allEntries = SQLiteSerializable.select(type, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return allEntries.stream()
                .map(entry -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(SQLiteSerializable.getPrimaryKeyValue(entry).toString(),
                                entry);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }
    // Implementation of Map interface methods would go here, using the
    // SQLiteSerializable methods

}
