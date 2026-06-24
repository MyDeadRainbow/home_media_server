package com.hms.annotated.sql;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.hms.dao.Database;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class SqlRecordDao<T extends Object> {
    private static final Map<Class<?>, SqlRecordDao<?>> factories = new HashMap<>();

    static {
        // get all classes annotated with @SqlRecord and create factories for them
        try {
            ClassGraph classGraph = new ClassGraph().enableAllInfo().acceptPackages("com.hms.*");
            try (ScanResult scanResult = classGraph.scan()) {
                List<ClassInfo> sqlRecordClasses = scanResult.getClassesWithAnnotation(TableRecord.class.getName());
                for (ClassInfo classInfo : sqlRecordClasses) {
                    Class<?> clazz = classInfo.loadClass();
                    factories.put(clazz, new SqlRecordDao<>(clazz));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing SqlRecordDao", e);
        }
    }

    public static <T> SqlRecordDao<T> getFactory(Class<T> recordClass) {
        @SuppressWarnings("unchecked")
        SqlRecordDao<T> factory = (SqlRecordDao<T>) factories.get(recordClass);
        if (factory == null) {
            throw new RuntimeException("No factory found for class: " + recordClass.getName());
        }
        return factory;
    }

    private final String dbName;
    private final Class<T> recordClass;
    private final RecordComponent[] fields;
    private final Constructor<T> constructor;
    private final Statement statement;

    private SqlRecordDao(Class<T> recordClass) {
        this.recordClass = recordClass;
        this.fields = recordClass.getRecordComponents();
        TableRecord dbNameAnnotation = recordClass.getAnnotation(TableRecord.class);
        if (dbNameAnnotation == null) {
            throw new RuntimeException("No @SqlRecord annotation found for " + recordClass.getName());
        }
        this.dbName = dbNameAnnotation.dbName();

        try {
            this.constructor = recordClass.getDeclaredConstructor(getParameters(recordClass));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No suitable constructor found for " + recordClass.getName(), e);
        }

        this.statement = new StatementBuilder()
                .addRecordComponents(fields)
                .build(recordClass.getSimpleName());

        // Create table if not exists
        try (var connection = Database.getConnection(dbName);
                var stmt = connection.createStatement();) {
            stmt.executeUpdate(statement.toCreateTable());
        } catch (Exception e) {
            throw new RuntimeException("Error creating table for " + recordClass.getName(), e);
        }
    }

    private static Class<?>[] getParameters(Class<?> recordClass) {
        return recordClass.getRecordComponents() != null
                ? java.util.Arrays.stream(recordClass.getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
                : new Class<?>[0];
    }

    private RecordComponent getPrimaryKeyField() {
        for (RecordComponent field : fields) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                return field;
            }
        }
        throw new RuntimeException("No primary key field found for " + recordClass.getName());
    }

    public List<T> select(Map<String, Object> whereConditions) {
        StatementValues sql = statement.toSelectStatement(whereConditions);

        List<T> results = new ArrayList<>();
        try (var connection = Database.getConnection(dbName);
                var preparedStatement = connection.prepareStatement(sql.sql())) {
            for (int i = 0; i < sql.parameters().size(); i++) {
                preparedStatement.setObject(i + 1, sql.parameters().get(i));
            }
            var resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                List<SqlField> fields = statement.getFields();
                Object[] values = new Object[fields.size()];
                for (int i = 0; i < fields.size(); i++) {
                    SqlField field = fields.get(i);
                    if (field.recordComponent.isAnnotationPresent(OneToMany.class)) {
                        Class<?> childClass = (Class<?>) ((java.lang.reflect.ParameterizedType) field.recordComponent
                                .getGenericType()).getActualTypeArguments()[0];

                        SqlRecordDao<?> childFactory = SqlRecordDao.getFactory(childClass);

                        List<?> childRecords = childFactory
                                .select(Map.of(getPrimaryKeyField().getName(),
                                        resultSet.getObject(getPrimaryKeyField().getName())));

                        if (List.class.isAssignableFrom(field.recordComponent.getType())) {
                            values[i] = childRecords;
                        } else if (!childRecords.isEmpty()) {
                            values[i] = childRecords.get(0);
                        } else {
                            values[i] = null;
                        }
                    } else if (field.recordComponent.isAnnotationPresent(OneToOne.class)) {
                        Class<?> siblingClass = field.recordComponent.getType();

                        SqlRecordDao<?> siblingFactory = SqlRecordDao.getFactory(siblingClass);
                        String siblingFieldName = Arrays.stream(siblingClass.getRecordComponents())
                                .filter(r -> r.isAnnotationPresent(PrimaryKey.class)).findFirst()
                                .map(RecordComponent::getName)
                                .orElseThrow(() -> new RuntimeException("No primary key found for sibling record"));

                        List<?> siblingRecords = siblingFactory
                                .select(Map.of(siblingFieldName,
                                        resultSet.getObject(siblingFieldName)));
                        if (List.class.isAssignableFrom(field.recordComponent.getType())) {
                            values[i] = siblingRecords;
                        } else if (!siblingRecords.isEmpty()) {
                            values[i] = siblingRecords.get(0);
                        } else {
                            values[i] = null;
                        }
                    } else if (field.recordComponent.getType().isEnum()) {
                        String enumValue = resultSet.getString(field.recordComponent.getName());
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        Object enumConstant = Enum.valueOf((Class<Enum>) field.recordComponent.getType(),
                                enumValue);
                        values[i] = enumConstant;
                    } else if (field.recordComponent.getType() == LocalDateTime.class) {
                        String dateTimeString = resultSet.getString(field.recordComponent.getName());
                        values[i] = LocalDateTime.parse(dateTimeString);
                    } else {
                        values[i] = resultSet.getObject(field.recordComponent.getName());
                    }
                }
                T record = constructor.newInstance(values);
                results.add(record);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error selecting records from " + recordClass.getName(), e);
        }
        return results;
    }

    public void insert(T record) {
        StatementValues sql = statement.toInsertStatement(record);
        try (var connection = Database.getConnection(dbName);
                var preparedStatement = connection.prepareStatement(sql.sql())) {
            for (int i = 0; i < sql.parameters().size(); i++) {
                preparedStatement.setObject(i + 1, sql.parameters().get(i));
            }
            preparedStatement.executeUpdate();

            for (SqlField field : statement.getFields()) {
                RecordComponent component = field.recordComponent;
                if (component.isAnnotationPresent(OneToMany.class)) {
                    Object childRecord = component.getAccessor().invoke(record);
                    if (childRecord instanceof List) {
                        List<?> childRecords = (List<?>) childRecord;
                        for (Object child : childRecords) {
                            SqlRecordDao childFactory = SqlRecordDao
                                    .getFactory(child.getClass());
                            childFactory.insert(child);
                        }
                    } else if (childRecord != null) {
                        SqlRecordDao childFactory = SqlRecordDao
                                .getFactory(childRecord.getClass());
                        childFactory.insert(childRecord);
                    }
                } else if (component.isAnnotationPresent(OneToOne.class)) {
                    Object siblingRecord = component.getAccessor().invoke(record);
                    if (siblingRecord instanceof List) {
                        List<?> siblingRecords = (List<?>) siblingRecord;
                        for (Object sibling : siblingRecords) {
                            SqlRecordDao siblingFactory = SqlRecordDao
                                    .getFactory(sibling.getClass());
                            siblingFactory.insert(sibling);
                        }
                    } else if (siblingRecord != null) {
                        SqlRecordDao siblingFactory = SqlRecordDao
                                .getFactory(siblingRecord.getClass());
                        siblingFactory.insert(siblingRecord);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inserting record into " + recordClass.getName(), e);
        }

    }

    public void update(T record) {
        StatementValues sql = statement.toUpdateStatement(record);
        try (var connection = Database.getConnection(dbName);
                var preparedStatement = connection.prepareStatement(sql.sql())) {
            for (int i = 0; i < sql.parameters().size(); i++) {
                preparedStatement.setObject(i + 1, sql.parameters().get(i));
            }
            preparedStatement.executeUpdate();

            for (SqlField field : statement.getFields()) {
                RecordComponent component = field.recordComponent;
                if (component.isAnnotationPresent(OneToMany.class)) {
                    Object childRecord = component.getAccessor().invoke(record);
                    if (childRecord instanceof List) {
                        List<?> childRecords = (List<?>) childRecord;
                        for (Object child : childRecords) {
                            SqlRecordDao childFactory = SqlRecordDao
                                    .getFactory(child.getClass());
                            childFactory.update(child);
                        }
                    } else if (childRecord != null) {
                        SqlRecordDao childFactory = SqlRecordDao
                                .getFactory(childRecord.getClass());
                        childFactory.update(childRecord);
                    }
                } else if (component.isAnnotationPresent(OneToOne.class)) {
                    Object siblingRecord = component.getAccessor().invoke(record);
                    if (siblingRecord instanceof List) {
                        List<?> siblingRecords = (List<?>) siblingRecord;
                        for (Object sibling : siblingRecords) {
                            SqlRecordDao siblingFactory = SqlRecordDao
                                    .getFactory(sibling.getClass());
                            siblingFactory.update(sibling);
                        }
                    } else if (siblingRecord != null) {
                        SqlRecordDao siblingFactory = SqlRecordDao
                                .getFactory(siblingRecord.getClass());
                        siblingFactory.update(siblingRecord);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating record in " + recordClass.getName(), e);
        }
    }

    public void delete(T record) {
        StatementValues StatementValues = statement.toDeleteStatement(record);

        for (SqlField field : statement.getFields()) {
            RecordComponent component = field.recordComponent;
            if (component.isAnnotationPresent(OneToMany.class)) {
                Object childRecord;
                try {
                    childRecord = component.getAccessor().invoke(record);
                } catch (Exception e) {
                    throw new RuntimeException("Error accessing child record for deletion in " + recordClass.getName(),
                            e);
                }
                if (childRecord instanceof List) {
                    List<?> childRecords = (List<?>) childRecord;
                    for (Object child : childRecords) {
                        SqlRecordDao childFactory = SqlRecordDao
                                .getFactory(child.getClass());
                        childFactory.delete(child);
                    }
                } else if (childRecord != null) {
                    SqlRecordDao childFactory = SqlRecordDao
                            .getFactory(childRecord.getClass());
                    childFactory.delete(childRecord);
                }
            } else if (component.isAnnotationPresent(OneToOne.class)) {
                Object siblingRecord;
                try {
                    siblingRecord = component.getAccessor().invoke(record);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Error accessing sibling record for deletion in " + recordClass.getName(),
                            e);
                }
                if (siblingRecord instanceof List) {
                    List<?> siblingRecords = (List<?>) siblingRecord;
                    for (Object sibling : siblingRecords) {
                        SqlRecordDao siblingFactory = SqlRecordDao
                                .getFactory(sibling.getClass());
                        siblingFactory.delete(sibling);
                    }
                } else if (siblingRecord != null) {
                    SqlRecordDao siblingFactory = SqlRecordDao
                            .getFactory(siblingRecord.getClass());
                    siblingFactory.delete(siblingRecord);
                }
            }
        }

        try (var connection = Database.getConnection(dbName);
                var preparedStatement = connection.prepareStatement(StatementValues.sql())) {
            for (int i = 0; i < StatementValues.parameters().size(); i++) {
                preparedStatement.setObject(i + 1, StatementValues.parameters().get(i));
            }
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting record from " + recordClass.getName(), e);
        }
    }
}

class Statement {
    private final String tableName;
    private final List<SqlField> fields;

    public Statement(String tableName, List<SqlField> fields) {
        this.tableName = tableName;
        this.fields = fields;
    }

    private SqlField getPrimaryKeyField() {
        for (SqlField field : fields) {
            for (Constraint constraint : field.constraints) {
                if (constraint instanceof PrimaryKeyConstraint) {
                    return field;
                }
            }
        }
        throw new RuntimeException("No primary key field found for table " + tableName);
    }

    List<SqlField> getFields() {
        return fields;
    }

    public String toCreateTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

        StringBuilder fieldDefinitions = new StringBuilder();
        StringBuilder constraints = new StringBuilder();
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(OneToMany.class)).toList();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            fieldDefinitions.append(field.toFieldDefinition());
            for (Constraint constraint : field.constraints) {
                constraints.append(", ").append(constraint.toSql());
            }

            if (i < printableFields.size() - 1) {
                fieldDefinitions.append(", ");
            }
        }
        sb.append(fieldDefinitions);
        sb.append(constraints);
        sb.append(");");
        return sb.toString();
    }

    public StatementValues toSelectStatement(Map<String, Object> conditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(OneToMany.class)).toList();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            sb.append(field.name);
            if (i < printableFields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(" FROM ").append(tableName).append(toWhereClause(conditions));
        return new StatementValues(sb.toString(), new ArrayList<>(conditions.values()));
    }

    public StatementValues toInsertStatement(Object record) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(OneToMany.class)).toList();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            String name = field.name;
            if (field.recordComponent.isAnnotationPresent(OneToOne.class)) {
                name = Arrays.stream(field.recordComponent.getType().getRecordComponents())
                        .filter(r -> r.isAnnotationPresent(PrimaryKey.class))
                        .map(RecordComponent::getName)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No primary key found for sibling record"));
            }
            sb.append(name);
            if (i < printableFields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") VALUES (");
        for (int i = 0; i < printableFields.size(); i++) {
            sb.append("?");
            if (i < printableFields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(");");
        List<Object> parameters = new ArrayList<>();
        for (SqlField field : printableFields) {
            try {
                if (field.recordComponent.isAnnotationPresent(OneToOne.class)) {
                    Object siblingRecord = field.getValue(record);
                    if (siblingRecord != null) {
                        SqlField primaryKeyField = Arrays.stream(siblingRecord.getClass().getRecordComponents())
                                .filter(r -> r.isAnnotationPresent(PrimaryKey.class))
                                .map(r -> SqlField.fromRecordComponent(r))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("No primary key found for sibling record"));
                        parameters.add(primaryKeyField.getValue(siblingRecord));
                    } else {
                        parameters.add(null);
                    }
                } else {
                    parameters.add(field.getValue(record));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error getting value for field " + field.name, e);
            }
        }
        return new StatementValues(sb.toString(), parameters);
    }

    public StatementValues toUpdateStatement(Object record) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(tableName).append(" SET ");
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(OneToMany.class)).toList();
        List<Object> parameters = new ArrayList<>();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            if (field.equals(getPrimaryKeyField())) {
                continue; // Skip primary key field in SET clause
            }
            String name = field.name;
            if (field.recordComponent.isAnnotationPresent(OneToOne.class)) {
                name = Arrays.stream(field.recordComponent.getType().getRecordComponents())
                        .filter(r -> r.isAnnotationPresent(PrimaryKey.class))
                        .map(RecordComponent::getName)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No primary key found for sibling record"));
            }
            sb.append(name).append(" = ?");
            try {
                if (field.recordComponent.isAnnotationPresent(OneToOne.class)) {
                    Object siblingRecord = field.getValue(record);
                    if (siblingRecord != null) {
                        SqlField primaryKeyField = Arrays.stream(siblingRecord.getClass().getRecordComponents())
                                .filter(r -> r.isAnnotationPresent(PrimaryKey.class))
                                .map(r -> SqlField.fromRecordComponent(r))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("No primary key found for sibling record"));
                        parameters.add(primaryKeyField.getValue(siblingRecord));
                    } else {
                        parameters.add(null);
                    }
                } else {
                    parameters.add(field.getValue(record));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error getting value for field " + field.name, e);
            }
            if (i < printableFields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(" WHERE ").append(getPrimaryKeyField().name).append(" = ?;");
        try {
            parameters.add(getPrimaryKeyField().getValue(record));
        } catch (Exception e) {
            throw new RuntimeException("Error getting value for primary key field " + getPrimaryKeyField().name, e);
        }
        return new StatementValues(sb.toString(), parameters);
    }

    public StatementValues toDeleteStatement(Object record) {
        String sql = "DELETE FROM " + tableName + " WHERE " + getPrimaryKeyField().name + " = ?;";
        List<Object> parameters = new ArrayList<>();
        try {
            parameters.add(getPrimaryKeyField().getValue(record));
        } catch (Exception e) {
            throw new RuntimeException("Error getting value for primary key field " + getPrimaryKeyField().name, e);
        }
        return new StatementValues(sql, parameters);
    }

    public String toWhereClause(Map<String, Object> conditions) {
        StringBuilder sb = new StringBuilder();
        if (conditions != null && !conditions.isEmpty()) {
            sb.append(" WHERE ");
            int i = 0;
            for (String field : conditions.keySet()) {
                sb.append(field).append(" = ?");
                if (i < conditions.size() - 1) {
                    sb.append(" AND ");
                }
                i++;
            }
        }
        return sb.toString();
    }
}

class StatementBuilder {
    List<SqlField> fields = new ArrayList<>();

    public StatementBuilder addRecordComponents(RecordComponent... components) {
        for (RecordComponent component : components) {
            fields.add(SqlField.fromRecordComponent(component));
        }
        return this;
    }

    public StatementBuilder addRecordComponent(RecordComponent component) {
        fields.add(SqlField.fromRecordComponent(component));
        return this;
    }

    public StatementBuilder addFields(SqlField... fields) {
        for (SqlField field : fields) {
            this.fields.add(field);
        }
        return this;
    }

    public StatementBuilder addField(SqlField field) {
        fields.add(field);
        return this;
    }

    public Statement build(String tableName) {
        return new Statement(tableName, fields);
    }
}


class SqlField {
    String name;
    SqlFieldType type;
    List<Flag> flags;
    List<Constraint> constraints;
    List<Annotation> annotations;
    RecordComponent recordComponent;
    // ValueGetter valueGetter;

    public SqlField(String name, SqlFieldType type, List<Flag> flags, List<Constraint> constraints,
            List<Annotation> annotations, RecordComponent recordComponent) {
        this.name = name;
        this.type = type;
        this.flags = List.copyOf(flags);
        this.constraints = List.copyOf(constraints);
        this.annotations = List.copyOf(annotations);
        this.recordComponent = recordComponent;
        // this.valueGetter = valueGetter;
    }

    public Object getValue(Object record) {
        try {
            return recordComponent.getAccessor().invoke(record);
        } catch (Exception e) {
            throw new RuntimeException("Error getting value for field " + name, e);
        }
    }

    public static String getPrimaryKeyFieldName(Class<?> recordClass) {
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (component.isAnnotationPresent(PrimaryKey.class)) {
                return component.getName();
            }
        }
        throw new RuntimeException("No primary key field found for " + recordClass.getName());
    }

    public static SqlField fromRecordComponent(RecordComponent component) {
        List<Flag> flags = new ArrayList<>();
        if (component.isAnnotationPresent(NotNull.class)) {
            flags.add(new NotNullFlag());
        }

        if (component.isAnnotationPresent(OneToMany.class)) {
            if (List.class.isAssignableFrom(component.getType())) {
                Class<?> genericType = (Class<?>) ((java.lang.reflect.ParameterizedType) component.getGenericType())
                        .getActualTypeArguments()[0];
                if (genericType.isAnnotationPresent(TableRecord.class)) {
                    return new SqlField(component.getName(), SqlFieldType.RECORD, flags,
                            List.of(), List.of(component.getAnnotations()), component);
                } else {
                    throw new RuntimeException("Unsupported generic type for List: " + genericType.getName());
                }
            } else {
                if (component.getType().isAnnotationPresent(TableRecord.class)) {
                    return new SqlField(component.getName(), SqlFieldType.RECORD, flags,
                            List.of(), List.of(component.getAnnotations()), component);
                } else {
                    throw new RuntimeException(
                            "Unsupported type for @ChildLink: " + component.getType().getName());
                }
            }
        }

        if (component.isAnnotationPresent(ManyToOne.class)) {
            ManyToOne parentLink = component.getAnnotation(ManyToOne.class);
            if (parentLink.toOne().isAnnotationPresent(TableRecord.class)) {
                SqlFieldType type = mapJavaTypeToSqlType(component.getType());

                Class<?> referencedClass = parentLink.toOne();
                String referencedTable = referencedClass.getSimpleName();

                return new SqlField(component.getName(), type, flags,
                        List.of(new ForeignKeyConstraint(component.getName(), referencedTable,
                                getPrimaryKeyFieldName(referencedClass))),
                        List.of(component.getAnnotations()), component);
            } else {
                throw new RuntimeException(
                        "Unsupported type for @ParentLink: " + parentLink.toOne().getName());
            }
        }

        if (component.isAnnotationPresent(OneToOne.class)) {
            // SiblingLink siblingLink = component.getAnnotation(SiblingLink.class);
            if (component.getType().isAnnotationPresent(TableRecord.class)) {
                // SqlFieldType type = mapJavaTypeToSqlType(component.getType());

                Class<?> referencedClass = component.getType();
                String fieldName = Arrays.stream(referencedClass.getRecordComponents())
                        .filter(r -> r.isAnnotationPresent(PrimaryKey.class)).findFirst().map(RecordComponent::getName)
                        .orElseThrow(() -> new RuntimeException("No primary key found for sibling record"));
                String referencedTable = component.getType().getSimpleName();

                return new SqlField(fieldName, SqlFieldType.RECORD, flags,
                        List.of(new ForeignKeyConstraint(fieldName, referencedTable,
                                getPrimaryKeyFieldName(referencedClass))),
                        List.of(component.getAnnotations()), component);
            } else {
                throw new RuntimeException(
                        "Unsupported type for @SiblingLink: " + component.getType().getName());
            }
        }
        String name = component.getName();
        SqlFieldType type = mapJavaTypeToSqlType(component.getType());

        List<Constraint> constraints = new ArrayList<>();
        if (component.isAnnotationPresent(PrimaryKey.class)) {
            constraints.add(new PrimaryKeyConstraint(name));
        }

        return new SqlField(name, type, flags, constraints, List.of(component.getAnnotations()),
                component);
    }

    private static SqlFieldType mapJavaTypeToSqlType(Class<?> javaType) {
        return SqlFieldType.fromJavaType(javaType);
    }

    public String toFieldDefinition() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(type.toSql());
        for (Flag flag : flags) {
            sb.append(" ").append(flag.toSql());
        }
        return sb.toString();
    }
}

enum SqlFieldType {
    TEXT("TEXT", c -> c == String.class || c.isEnum()),
    INTEGER("INTEGER", c -> c == int.class || c == Integer.class),
    BIGINT("BIGINT", c -> c == long.class || c == Long.class),
    REAL("REAL", c -> c == double.class || c == Double.class),
    BOOLEAN("BOOLEAN", c -> c == boolean.class || c == Boolean.class),
    DATE("DATE", c -> c == LocalDateTime.class),
    RECORD("RECORD", c -> false);

    final String sql;
    final Predicate<Class<?>> predicate;

    private SqlFieldType(String sql, Predicate<Class<?>> predicate) {
        this.sql = sql;
        this.predicate = predicate;
    }

    String toSql() {
        return sql;
    }

    static SqlFieldType fromJavaType(Class<?> javaType) {
        for (SqlFieldType type : SqlFieldType.values()) {
            if (type.predicate.test(javaType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported Java type: " + javaType.getName());
    }
}

interface Flag {
    String toSql();
}

class NotNullFlag implements Flag {
    @Override
    public String toSql() {
        return "NOT NULL";
    }
}

interface Constraint {
    String toSql();
}

class PrimaryKeyConstraint implements Constraint {
    private final String fieldName;

    public PrimaryKeyConstraint(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toSql() {
        return "PRIMARY KEY (" + fieldName + ")";
    }
}

class ForeignKeyConstraint implements Constraint {
    private final String fieldName;
    private final String referenceTable;
    private final String referenceField;

    public ForeignKeyConstraint(String fieldName, String referenceTable, String referenceField) {
        this.fieldName = fieldName;
        this.referenceTable = referenceTable;
        this.referenceField = referenceField;
    }

    @Override
    public String toSql() {
        return "FOREIGN KEY (" + fieldName + ") REFERENCES " + referenceTable + "(" + referenceField + ")";
    }
}

class UniqueConstraint implements Constraint {
    private final String fieldName;

    public UniqueConstraint(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toSql() {
        return "UNIQUE (" + fieldName + ")";
    }
}

class CheckConstraint implements Constraint {
    private final String condition;

    public CheckConstraint(String condition) {
        this.condition = condition;
    }

    @Override
    public String toSql() {
        return "CHECK (" + condition + ")";
    }
}