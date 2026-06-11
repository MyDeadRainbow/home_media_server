package com.hms.shared.dao;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public interface SQLiteSerializable {

    public String getDbPath();

    public String getTableName();

    public default String toCreateTableStatement() {
        // 'this' refers to the concrete object implementing the interface
        Class<?> concreteClass = this.getClass();

        // Use getDeclaredFields to grab public, private, and protected fields
        Field[] fields = concreteClass.getDeclaredFields();

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append(" (");

        Iterator<Field> fieldIterator = java.util.Arrays.stream(fields)
                .filter(f -> !f.isAnnotationPresent(IgnoreField.class))
                .iterator();
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            sql.append(field.getName()).append(" ").append(mapJavaTypeToSQLiteType(field.getType()));
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                sql.append(" PRIMARY KEY");
            }
            if (fieldIterator.hasNext()) {
                sql.append(", ");
            }
        }

        sql.append(")");
        return sql.toString();
    }

    private static String mapJavaTypeToSQLiteType(Class<?> javaType) {
        if (javaType == String.class) {
            return "TEXT";
        } else if (javaType == int.class || javaType == Integer.class) {
            return "INTEGER";
        } else if (javaType == long.class || javaType == Long.class) {
            return "BIGINT";
        } else if (javaType == double.class || javaType == Double.class) {
            return "REAL";
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            return "BOOLEAN";
        } else {
            throw new IllegalArgumentException("Unsupported Java type: " + javaType.getName());
        }
    }

    public default String toInsertStatement() {
        // 'this' refers to the concrete object implementing the interface
        Class<?> concreteClass = this.getClass();

        // Use getDeclaredFields to grab public, private, and protected fields
        Field[] fields = concreteClass.getDeclaredFields();

        StringBuilder insert = new StringBuilder();
        insert.append("INSERT INTO ").append(getTableName()).append(" (");

        StringBuilder fieldNames = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        Iterator<Field> fieldIterator = java.util.Arrays.stream(fields)
                .filter(f -> !f.isAnnotationPresent(IgnoreField.class))
                .iterator();
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            fieldNames.append(field.getName());
            placeholders.append("?");
            if (fieldIterator.hasNext()) {
                fieldNames.append(", ");
                placeholders.append(", ");
            }
        }

        insert.append(fieldNames).append(") VALUES (").append(placeholders).append(")");
        return insert.toString();
    }

    public default void insert() throws DBFileNotFoundException, GetConnectionException, SQLException {
        try (var conn = Database.getConnection(getDbPath());) {
            try (var createStmt = conn.prepareStatement(toCreateTableStatement());) {
                createStmt.execute();
            }
            try (var pstmt = conn.prepareStatement(toInsertStatement())) {
                Field[] fields = this.getClass().getDeclaredFields();
                int index = 1;
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(IgnoreField.class)) {
                        field.setAccessible(true);
                        try {
                            pstmt.setObject(index++, field.get(this));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                pstmt.executeUpdate();
            }
        }
    }

    public static <T extends SQLiteSerializable> T getById(Class<T> clazz, Object id)
            throws DBFileNotFoundException, GetConnectionException, SQLException {

        T instance = null;
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            instance = (T) constructor.newInstance(new Object[constructor.getParameterCount()]);
        } catch (Exception e) {
            throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
        }

        try (var conn = Database.getConnection(instance.getDbPath());) {
            String tableName = instance.getTableName();
            String pkFieldName = clazz.getDeclaredFields().length > 0
                    ? Arrays.stream(clazz.getDeclaredFields())
                            .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
                            .map(f -> f.getName())
                            .findFirst()
                            .orElse("id")
                    : "id"; // Default to 'id' if no fields are declared
            String sql = "SELECT * FROM " + tableName + " WHERE " + pkFieldName + " = ?";
            try (var pstmt = conn.prepareStatement(sql)) {
                pstmt.setObject(1, id);
                try (var rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Field[] fields = clazz.getDeclaredFields();
                        for (Field field : fields) {
                            if (!field.isAnnotationPresent(IgnoreField.class)) {
                                field.setAccessible(true);
                                Object value = rs.getObject(field.getName());
                                try {
                                    field.set(instance, value);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        return instance;
                    } else {
                        throw new DBFileNotFoundException("Record with id " + id + " not found in table " + tableName);
                    }
                }
            }
        }
    }

    public static <T extends SQLiteSerializable> List<T> ListAll(Class<T> clazz)
            throws DBFileNotFoundException, GetConnectionException, SQLException {

        T instance = null;
        Constructor<?> constructor;
        try {
            constructor = clazz.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            instance = (T) constructor.newInstance(new Object[constructor.getParameterCount()]);
        } catch (Exception e) {
            throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
        }

        List<T> results = new ArrayList<>();
        try (var conn = Database.getConnection(instance.getDbPath());) {
            String tableName = instance.getTableName();
            String sql = "SELECT * FROM " + tableName;
            try (var pstmt = conn.prepareStatement(sql); var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // try {
                    // rowInstance = (T) constructor.newInstance(new
                    // Object[constructor.getParameterCount()]);
                    // } catch (Exception e) {
                    // throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
                    // }
                    Field[] fields = clazz.getDeclaredFields();
                    List<Object> fieldValues = new ArrayList<>();
                    for (Field field : fields) {
                        if (!field.isAnnotationPresent(IgnoreField.class)) {
                            field.setAccessible(true);
                            Object value = rs.getObject(field.getName());
                            fieldValues.add(value);
                            // try {
                            // field.set(rowInstance, value);
                            // } catch (IllegalAccessException e) {
                            // throw new RuntimeException(e);
                            // }
                        }
                    }
                    T rowInstance;
                    try {
                        rowInstance = (T) constructor.newInstance(fieldValues.toArray(new Object[0]));
                    } catch (Exception e) {
                        throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
                    }
                    results.add(rowInstance);
                }
            }
        }
        return results;
    }
}
