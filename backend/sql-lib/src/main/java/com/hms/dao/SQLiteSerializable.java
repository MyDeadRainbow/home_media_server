package com.hms.dao;

// import java.lang.reflect.Constructor;
// import java.lang.reflect.Field;
// import java.lang.reflect.InvocationTargetException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.sql.Date;
// import java.sql.SQLException;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Iterator;
// import java.util.List;
// import java.util.Map;

// import com.google.common.collect.Iterables;

// public interface SQLiteSerializable {

//     public abstract String getDbPath();

//     public abstract String getTableName();

//     private void ensureTableExists() throws DBFileNotFoundException, GetConnectionException, SQLException {
//         try (var conn = Database.getConnection(getDbPath());
//                 var stmt = conn.prepareStatement(toCreateTableStatement())) {
//             stmt.execute();
//         }
//     }

//     public default String toCreateTableStatement() {
//         // 'this' refers to the concrete object implementing the interface
//         Class<?> concreteClass = this.getClass();

//         // Use getDeclaredFields to grab public, private, and protected fields
//         Field[] fields = concreteClass.getDeclaredFields();

//         StringBuilder sql = new StringBuilder();
//         sql.append("CREATE TABLE IF NOT EXISTS ").append(getTableName()).append(" (");

//         StringBuilder fieldDefinitions = new StringBuilder();
//         StringBuilder foreignKeyConstraints = new StringBuilder();

//         Iterator<Field> fieldIterator = java.util.Arrays.stream(fields)
//                 .filter(f -> !f.isAnnotationPresent(IgnoreField.class)
//                         && !f.isAnnotationPresent(ChildKey.class))
//                 .iterator();
//         Iterator<Field> foreignKeyIterator = java.util.Arrays.stream(fields)
//                 .filter(f -> f.isAnnotationPresent(ParentKey.class))
//                 .iterator();
//         while (fieldIterator.hasNext()) {
//             Field field = fieldIterator.next();
//             if (field.isAnnotationPresent(ChildKey.class)) {
//                 // Skip child key fields in the parent table
//                 continue;
//             }
//             fieldDefinitions.append(field.getName()).append(" ").append(mapJavaTypeToSQLiteType(field));
//             if (field.isAnnotationPresent(PrimaryKey.class)) {
//                 fieldDefinitions.append(" PRIMARY KEY");
//             }

//             if (fieldIterator.hasNext()) {
//                 fieldDefinitions.append(", ");
//             }
//         }

//         while (foreignKeyIterator.hasNext()) {

//             Field field = foreignKeyIterator.next();
//             ParentKey parentKeyAnnotation = field.getAnnotation(ParentKey.class);
//             Class<? extends SQLiteSerializable> referencedClass = parentKeyAnnotation.referencedClass();
//             String referencedTableName;
//             try {
//                 Constructor<?> constructor = referencedClass.getDeclaredConstructors()[0];
//                 constructor.setAccessible(true);
//                 SQLiteSerializable referencedInstance = (SQLiteSerializable) constructor
//                         .newInstance(new Object[constructor.getParameterCount()]);
//                 referencedTableName = referencedInstance.getTableName();
//             } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
//                     | InvocationTargetException e) {
//                 throw new RuntimeException("Failed to instantiate referenced class: " + referencedClass.getName(), e);
//             }
//             Field pkField = getPrimaryKeyField(referencedClass);
//             foreignKeyConstraints.append("FOREIGN KEY(").append(field.getName()).append(") REFERENCES ")
//                     .append(referencedTableName).append("(").append(pkField.getName()).append(")");
//             if (foreignKeyIterator.hasNext()) {
//                 foreignKeyConstraints.append(", ");
//             }
//         }

//         sql.append(fieldDefinitions);
//         if (foreignKeyConstraints.length() > 0) {
//             sql.append(", ").append(foreignKeyConstraints);
//         }
//         sql.append(")");
//         return sql.toString();
//     }

//     private static String mapJavaTypeToSQLiteType(Field field) {
//         Class<?> javaType = field.getType();
//         if (field.isAnnotationPresent(ParentKey.class)) {
//             return "TEXT"; // Assuming parent keys are stored as TEXT (UUIDs)
//         } else if (javaType == String.class || javaType.isEnum()) {
//             return "TEXT";
//         } else if (javaType == int.class || javaType == Integer.class) {
//             return "INTEGER";
//         } else if (javaType == long.class || javaType == Long.class) {
//             return "BIGINT";
//         } else if (javaType == double.class || javaType == Double.class) {
//             return "REAL";
//         } else if (javaType == boolean.class || javaType == Boolean.class) {
//             return "BOOLEAN";
//         } else if (javaType == Date.class) {
//             return "INTEGER";
//         } else {
//             throw new IllegalArgumentException("Unsupported Java type: " + javaType.getName());
//         }
//     }

//     public default String toInsertStatement() {
//         // 'this' refers to the concrete object implementing the interface
//         Class<?> concreteClass = this.getClass();

//         // Use getDeclaredFields to grab public, private, and protected fields
//         Field[] fields = concreteClass.getDeclaredFields();

//         StringBuilder insert = new StringBuilder();
//         insert.append("INSERT INTO ").append(getTableName()).append(" (");

//         StringBuilder fieldNames = new StringBuilder();
//         StringBuilder placeholders = new StringBuilder();

//         Iterator<Field> fieldIterator = java.util.Arrays.stream(fields)
//                 .filter(f -> !f.isAnnotationPresent(IgnoreField.class) && !f.isAnnotationPresent(ChildKey.class))
//                 .iterator();
//         while (fieldIterator.hasNext()) {
//             Field field = fieldIterator.next();
//             fieldNames.append(field.getName());
//             placeholders.append("?");
//             if (fieldIterator.hasNext()) {
//                 fieldNames.append(", ");
//                 placeholders.append(", ");
//             }
//         }

//         insert.append(fieldNames).append(") VALUES (").append(placeholders).append(")");
//         return insert.toString();
//     }

//     public default void insert() throws DBFileNotFoundException, GetConnectionException, SQLException {
//         ensureTableExists();
//         try (var conn = Database.getConnection(getDbPath());) {

//             try (var pstmt = conn.prepareStatement(toInsertStatement())) {
//                 Field[] fields = this.getClass().getDeclaredFields();
//                 int index = 1;
//                 for (Field field : fields) {
//                     if (field.isAnnotationPresent(IgnoreField.class)) {
//                         continue;
//                     }
//                     if (field.isAnnotationPresent(ChildKey.class)) {
//                         // Handle child key relationships (e.g., one-to-many)
//                         field.setAccessible(true);
//                         try {
//                             Object value = field.get(this);
//                             if (value instanceof List) {
//                                 List<?> childList = (List<?>) value;
//                                 for (Object child : childList) {
//                                     if (child instanceof SQLiteSerializable) {
//                                         ((SQLiteSerializable) child).insert();
//                                     } else {
//                                         throw new RuntimeException("Child objects must implement SQLiteSerializable");
//                                     }
//                                 }
//                             } else {
//                                 throw new RuntimeException("ChildKey fields must be of type List");
//                             }
//                         } catch (IllegalAccessException e) {
//                             throw new RuntimeException(e);
//                         }
//                     } else {
//                         field.setAccessible(true);
//                         try {
//                             Object value = field.get(this);
//                             if (value instanceof Enum) {
//                                 value = ((Enum<?>) value).name(); // Store enum as string
//                             }
//                             pstmt.setObject(index++, value);
//                         } catch (IllegalAccessException e) {
//                             throw new RuntimeException(e);
//                         }
//                     }

//                 }
//                 pstmt.executeUpdate();
//             }
//         }
//     }

//     /**
//      * This does not do anything with possible child key relationships. It assumes
//      * the entire object graph is being updated together, and that any necessary
//      * child inserts/updates will be handled separately.
//      * 
//      * @throws DBFileNotFoundException
//      * @throws GetConnectionException
//      * @throws SQLException
//      */
//     public default void update() throws DBFileNotFoundException, GetConnectionException, SQLException {
//         ensureTableExists();
//         try (var conn = Database.getConnection(getDbPath());) {
//             Field[] fields = this.getClass().getDeclaredFields();
//             StringBuilder sql = new StringBuilder();
//             sql.append("UPDATE ").append(getTableName()).append(" SET ");

//             String pkFieldName = Arrays.stream(fields)
//                     .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
//                     .map(Field::getName)
//                     .findFirst()
//                     .orElseThrow(() -> new RuntimeException("No primary key field found"));

//             List<Field> nonPkFields = new ArrayList<>();
//             for (Field field : fields) {
//                 if (!field.isAnnotationPresent(IgnoreField.class) && !field.getName().equals(pkFieldName)) {
//                     nonPkFields.add(field);
//                 }
//             }

//             for (int i = 0; i < nonPkFields.size(); i++) {
//                 sql.append(nonPkFields.get(i).getName()).append(" = ?");
//                 if (i < nonPkFields.size() - 1) {
//                     sql.append(", ");
//                 }
//             }
//             sql.append(" WHERE ").append(pkFieldName).append(" = ?");

//             try (var pstmt = conn.prepareStatement(sql.toString())) {
//                 int index = 1;
//                 for (Field field : nonPkFields) {
//                     field.setAccessible(true);
//                     try {
//                         Object value = field.get(this);
//                         if (value instanceof Enum) {
//                             value = ((Enum<?>) value).name(); // Store enum as string
//                         }
//                         pstmt.setObject(index++, value);
//                     } catch (IllegalAccessException e) {
//                         throw new RuntimeException(e);
//                     }
//                 }
//                 // Set primary key value
//                 Field pkField = this.getClass().getDeclaredField(pkFieldName);
//                 pkField.setAccessible(true);
//                 try {
//                     Object value = pkField.get(this);
//                     if (value instanceof Enum) {
//                         value = ((Enum<?>) value).name(); // Store enum as string
//                     }
//                     pstmt.setObject(index, value);
//                 } catch (IllegalAccessException e) {
//                     throw new RuntimeException(e);
//                 }
//                 pstmt.executeUpdate();
//             }
//         } catch (NoSuchFieldException e) {
//             throw new RuntimeException(e);
//         }
//     }

//     public default void delete() throws DBFileNotFoundException, GetConnectionException, SQLException {
//         ensureTableExists();
//         try (var conn = Database.getConnection(getDbPath());) {
//             Field[] fields = this.getClass().getDeclaredFields();

//             for (Field field : fields) {
//                 if (field.isAnnotationPresent(ChildKey.class)) {
//                     field.setAccessible(true);
//                     try {
//                         Object value = field.get(this);
//                         if (value instanceof List) {
//                             List<?> childList = (List<?>) value;
//                             for (Object child : childList) {
//                                 if (child instanceof SQLiteSerializable) {
//                                     ((SQLiteSerializable) child).delete();
//                                 } else {
//                                     throw new RuntimeException("Child objects must implement SQLiteSerializable");
//                                 }
//                             }
//                         } else {
//                             throw new RuntimeException("ChildKey fields must be of type List");
//                         }
//                     } catch (IllegalAccessException e) {
//                         throw new RuntimeException(e);
//                     }
//                 }
//             }
//             StringBuilder sql = new StringBuilder();
//             sql.append("DELETE FROM ").append(getTableName()).append(" WHERE ");

//             String pkFieldName = Arrays.stream(fields)
//                     .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
//                     .map(Field::getName)
//                     .findFirst()
//                     .orElseThrow(() -> new RuntimeException("No primary key field found"));

//             sql.append(pkFieldName).append(" = ?");

//             try (var pstmt = conn.prepareStatement(sql.toString())) {
//                 Field pkField = this.getClass().getDeclaredField(pkFieldName);
//                 pkField.setAccessible(true);
//                 try {
//                     Object value = pkField.get(this);
//                     if (value instanceof Enum) {
//                         value = ((Enum<?>) value).name(); // Store enum as string
//                     }
//                     pstmt.setObject(1, value);
//                 } catch (IllegalAccessException e) {
//                     throw new RuntimeException(e);
//                 }
//                 pstmt.executeUpdate();
//             }
//         } catch (NoSuchFieldException e) {
//             throw new RuntimeException(e);
//         }
//     }

//     public static <T extends SQLiteSerializable> T getById(Class<T> clazz, Object id)
//             throws DBFileNotFoundException, GetConnectionException, SQLException {

//         T instance = null;
//         Constructor<?> constructor;
//         try {
//             constructor = clazz.getDeclaredConstructors()[0];
//             constructor.setAccessible(true);
//             instance = (T) constructor.newInstance(new Object[constructor.getParameterCount()]);
//         } catch (Exception e) {
//             throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
//         }
//         ((SQLiteSerializable) instance).ensureTableExists();
//         try (var conn = Database.getConnection(instance.getDbPath());) {
//             String tableName = instance.getTableName();
//             String pkFieldName = clazz.getDeclaredFields().length > 0
//                     ? Arrays.stream(clazz.getDeclaredFields())
//                             .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
//                             .map(f -> f.getName())
//                             .findFirst()
//                             .orElse("id")
//                     : "id"; // Default to 'id' if no fields are declared
//             String sql = "SELECT * FROM " + tableName + " WHERE " + pkFieldName + " = ?";
//             try (var pstmt = conn.prepareStatement(sql)) {
//                 pstmt.setObject(1, id);
//                 try (var rs = pstmt.executeQuery()) {
//                     if (rs.next()) {
//                         Field[] fields = clazz.getDeclaredFields();
//                         List<Object> fieldValues = new ArrayList<>();
//                         for (Field field : fields) {
//                             if (field.isAnnotationPresent(IgnoreField.class)) {
//                                 continue;
//                             }

//                             field.setAccessible(true);
//                             Object value = null;
//                             if (field.isAnnotationPresent(ChildKey.class)) {
//                                 value = SQLiteSerializable.select(field.getAnnotation(ChildKey.class).referencedClass(),
//                                         Map.of(SQLiteSerializable.getPrimaryKeyField(
//                                                 field.getAnnotation(ChildKey.class).referencedClass()),
//                                                 rs.getObject(field.getName())));
//                             } else {
//                                 value = rs.getObject(field.getName());
//                                 if (field.getType().isEnum() && value instanceof String) {
//                                     value = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
//                                 }
//                                 if (field.getType() == Date.class && value instanceof Long) {
//                                     value = new Date((Long) value);
//                                 }
//                                 fieldValues.add(value);
//                             }
//                         }
//                         T rowInstance;
//                         try {
//                             rowInstance = (T) constructor.newInstance(fieldValues.toArray(new Object[0]));
//                         } catch (Exception e) {
//                             throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
//                         }
//                         return rowInstance;
//                     } else {
//                         throw new DBFileNotFoundException("Record with id " + id + " not found in table " + tableName);
//                     }
//                 }
//             }
//         }
//     }

//     public static <T extends SQLiteSerializable> List<T> select(Class<T> clazz, Map<Field, Object> filters)
//             throws DBFileNotFoundException, GetConnectionException, SQLException {

//         T instance = null;
//         Constructor<?> constructor;
//         try {
//             constructor = clazz.getDeclaredConstructors()[0];
//             constructor.setAccessible(true);
//             instance = (T) constructor.newInstance(new Object[constructor.getParameterCount()]);
//         } catch (Exception e) {
//             throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
//         }

//         if (filters.keySet().stream().anyMatch(field -> !field.getDeclaringClass().equals(clazz))) {
//             throw new IllegalArgumentException("All filter fields must belong to the class " + clazz.getName());
//         }

//         ((SQLiteSerializable) instance).ensureTableExists();
//         List<T> results = new ArrayList<>();
//         try (var conn = Database.getConnection(instance.getDbPath());) {
//             String tableName = instance.getTableName();
//             StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM " + tableName);
//             if (!filters.isEmpty()) {
//                 sqlBuilder.append(" WHERE ");
//                 List<String> conditions = new ArrayList<>();
//                 for (Field field : filters.keySet()) {
//                     conditions.add(field.getName() + " = ?");
//                 }
//                 sqlBuilder.append(String.join(" AND ", conditions));
//             }
//             String sql = sqlBuilder.toString();
//             try (var pstmt = conn.prepareStatement(sql)) {
//                 int index = 1;
//                 for (Object value : filters.values()) {
//                     pstmt.setObject(index++, value);
//                 }
//                 try (var rs = pstmt.executeQuery()) {
//                     while (rs.next()) {
//                         Field[] fields = clazz.getDeclaredFields();
//                         List<Object> fieldValues = new ArrayList<>();
//                         for (Field field : fields) {
//                             if (field.isAnnotationPresent(IgnoreField.class)) {
//                                 continue;
//                             }

//                             field.setAccessible(true);
//                             Object value = null;
//                             if (field.isAnnotationPresent(ChildKey.class)) {
//                                 try {
//                                     Class<? extends SQLiteSerializable> referencedClass = field.getAnnotation(ChildKey.class)
//                                             .referencedClass();

//                                     String referencedFieldName = field.getAnnotation(ChildKey.class).referencedField();
//                                     Field referencedField = getFieldByName(referencedClass, referencedFieldName);
//                                     value = SQLiteSerializable.select(
//                                             referencedClass,
//                                             Map.of(referencedField,
//                                                     SQLiteSerializable.getPrimaryKeyValue(instance)));
//                                     fieldValues.add(value);
//                                 } catch (Exception e) {
//                                     throw new RuntimeException(e);
//                                 }
//                             } else {
//                                 field.setAccessible(true);
//                                 value = rs.getObject(field.getName());
//                                 if (field.getType().isEnum() && value instanceof String) {
//                                     value = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
//                                 }
//                                 if (field.getType() == Date.class && value instanceof Long) {
//                                     value = new Date((Long) value);
//                                 }
//                                 fieldValues.add(value);
//                             }
//                         }
//                         T rowInstance;
//                         try {
//                             rowInstance = (T) constructor.newInstance(fieldValues.toArray(new Object[0]));
//                         } catch (Exception e) {
//                             throw new SQLException("Failed to instantiate class: " + clazz.getName(), e);
//                         }
//                         results.add(rowInstance);
//                     }
//                 }
//             }
//         }
//         return results;
//     }

//     public static Field getPrimaryKeyField(Class<?> clazz) {
//         return Arrays.stream(clazz.getDeclaredFields())
//                 .filter(f -> f.isAnnotationPresent(PrimaryKey.class))
//                 .findFirst()
//                 .orElseThrow(() -> new RuntimeException("No primary key field found in class " + clazz.getName()));
//     }

//     public static Field getFieldByName(Class<?> clazz, String fieldName) {
//         return Arrays.stream(clazz.getDeclaredFields())
//                 .filter(f -> f.getName().equals(fieldName))
//                 .findFirst()
//                 .orElseThrow(
//                         () -> new RuntimeException("Field " + fieldName + " not found in class " + clazz.getName()));
//     }

//     public static Object getPrimaryKeyValue(Object instance) {
//         Field pkField = getPrimaryKeyField(instance.getClass());
//         pkField.setAccessible(true);
//         try {
//             return pkField.get(instance);
//         } catch (IllegalAccessException e) {
//             throw new RuntimeException(e);
//         }
//     }

// }
