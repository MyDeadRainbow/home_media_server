package com.hms.dao;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLiteMap<T extends SQLiteRecord> implements Map<String, T> {

    private final SQLiteRecordDao<T> dao;

    public SQLiteMap(SQLiteRecordDao<T> dao) {
        this.dao = dao;
    }

    @Override
    public int size() {
        int size = 0;
        try {
            size = dao.select(Map.of()).size();
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
            return dao.select(Map.of(dao.getPrimaryKeyField(), key)).size() > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof SQLiteRecord)) {
            return false;
        }
        try {
            var record = (SQLiteRecord) value;
            var primaryKeyValue = record.getPrimaryKeyValue();

            return containsKey(primaryKeyValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T get(Object key) {
        try {
            var results = dao.select(Map.of(dao.getPrimaryKeyField(), key));
            if (results.size() > 0) {
                return results.get(0);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public T put(T value) {
        return put(value.getPrimaryKeyValue().toString(), value);
    }

    @Override
    public T put(String key, T value) {
        try {
            var existing = dao.get(key);
            if (existing != null) {
                dao.update(value);
                return existing;
            } else {
                dao.insert(value);
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T remove(Object key) {
        try {
            var existing = dao.get(key);
            if (existing != null) {
                dao.delete(existing);
                return existing;
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
            var allEntries = dao.select(Map.of());
            for (T entry : allEntries) {
                dao.delete(entry);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> keySet() {
        List<T> allEntries;
        try {
            allEntries = dao.select(Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return allEntries.stream()
                .map(entry -> {
                    try {
                        return dao.getPrimaryKeyValue(entry).toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Collection<T> values() {
        try {
            return dao.select(Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Entry<String, T>> entrySet() {
        List<T> allEntries;
        try {
            allEntries = dao.select(Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return allEntries.stream()
                .map(entry -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(dao.getPrimaryKeyValue(entry).toString(),
                                entry);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }

}
