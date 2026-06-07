package com.hms.shared.dao;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Iterator;

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
        
        StringBuilder stmt = new StringBuilder();
        stmt.append("BEGIN;");
        stmt.append(toCreateTableStatement()).append(";");

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
        stmt.append(insert).append(";");
        stmt.append("COMMIT;");
        return stmt.toString();
    }

    public default void insert() throws DBFileNotFoundException, GetConnectionException, SQLException {
        try (var conn = Database.getConnection(getDbPath()); var pstmt = conn.prepareStatement(toInsertStatement())) {
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
