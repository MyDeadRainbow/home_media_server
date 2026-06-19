package com.hms.shared.sql;

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

import com.hms.shared.dao.Database;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class SqlRecordFactory<T extends Object> {
    private static final Map<Class<?>, SqlRecordFactory<?>> factories = new HashMap<>();

    static {
        // get all classes annotated with @SqlRecord and create factories for them
        try {
            ClassGraph classGraph = new ClassGraph().enableAllInfo().acceptPackages("com.hms.*");
            try (ScanResult scanResult = classGraph.scan()) {
                List<ClassInfo> sqlRecordClasses = scanResult.getClassesWithAnnotation(SqlRecord.class.getName());
                for (ClassInfo classInfo : sqlRecordClasses) {
                    Class<?> clazz = classInfo.loadClass();
                    factories.put(clazz, new SqlRecordFactory<>(clazz));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing SqlRecordFactory", e);
        }
    }

    public static <T> SqlRecordFactory<T> getFactory(Class<T> recordClass) {
        @SuppressWarnings("unchecked")
        SqlRecordFactory<T> factory = (SqlRecordFactory<T>) factories.get(recordClass);
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

    private SqlRecordFactory(Class<T> recordClass) {
        this.recordClass = recordClass;
        this.fields = recordClass.getRecordComponents();
        SqlRecord dbNameAnnotation = recordClass.getAnnotation(SqlRecord.class);
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
        StatementRecord sql = statement.toSelectStatement(whereConditions);

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
                    if (field.recordComponent.isAnnotationPresent(ChildLink.class)) {
                        Class<?> childClass = (Class<?>) ((java.lang.reflect.ParameterizedType) field.recordComponent
                                .getGenericType()).getActualTypeArguments()[0];

                        SqlRecordFactory<?> childFactory = SqlRecordFactory.getFactory(childClass);

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
                    } else if (field.recordComponent.isAnnotationPresent(ParentLink.class)) {
                        // i think we actually want to ignore this field at first, then add the new
                        // instance of the parent record to the child record after we create it. This is
                        // because we want to avoid circular dependencies when creating records.
                        // Class<?> parentClass = field.recordComponent.getType();
                        // SqlRecordFactory<?> parentFactory = SqlRecordFactory.getFactory(parentClass);

                        // Object parentId = resultSet.getObject(field.recordComponent.getName());
                        // List<?> parentRecords = parentFactory.select(Map.of(
                        //         SqlField.getPrimaryKeyFieldName(parentClass), parentId));

                        // if (!parentRecords.isEmpty()) {
                        //     values[i] = parentRecords.get(0);
                        // } else {
                        //     values[i] = null;
                        // }
                        values[i] = null; // Set to null for now, will be set later after creating the record
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
        StatementRecord sql = statement.toInsertStatement(record);

    }

    public void update(T record) {
        StatementRecord sql = statement.toUpdateStatement(record);

    }

    public void delete(T record) {
        StatementRecord statementRecord = statement.toDeleteStatement(record);

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
    // List<SqlField> getChildFields() {
    // List<SqlField> childFields = new ArrayList<>();
    // for (SqlField field : fields) {
    // if (field.type == SqlFieldType.CHILD_RECORD) {
    // childFields.add(field);
    // }
    // }
    // return childFields;
    // }

    // private List<SqlField> getPrintableFields() {
    // List<SqlField> printableFields = new ArrayList<>();
    // for (SqlField field : fields) {
    // if (field.type != SqlFieldType.CHILD_RECORD) {
    // printableFields.add(field);
    // }
    // }
    // return printableFields;
    // }

    public String toCreateTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

        StringBuilder fieldDefinitions = new StringBuilder();
        StringBuilder constraints = new StringBuilder();
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(ChildLink.class)).toList();
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

    public StatementRecord toInsertStatement(Object record) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(ChildLink.class)).toList();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            sb.append(field.name);
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
                parameters.add(field.getValue(record));
            } catch (Exception e) {
                throw new RuntimeException("Error getting value for field " + field.name, e);
            }
        }
        return new StatementRecord(sb.toString(), parameters);
    }

    public StatementRecord toSelectStatement(Map<String, Object> conditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(ChildLink.class)).toList();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            sb.append(field.name);
            if (i < printableFields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(" FROM ").append(tableName).append(toWhereClause(conditions));
        return new StatementRecord(sb.toString(), new ArrayList<>(conditions.values()));
    }

    public StatementRecord toUpdateStatement(Object record) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(tableName).append(" SET ");
        List<SqlField> printableFields = fields.stream()
                .filter(f -> !f.recordComponent.isAnnotationPresent(ChildLink.class)).toList();
        List<Object> parameters = new ArrayList<>();
        for (int i = 0; i < printableFields.size(); i++) {
            SqlField field = printableFields.get(i);
            if (field.equals(getPrimaryKeyField())) {
                continue; // Skip primary key field in SET clause
            }
            sb.append(field.name).append(" = ?");
            try {
                parameters.add(field.getValue(record));
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
        return new StatementRecord(sb.toString(), parameters);
    }

    public StatementRecord toDeleteStatement(Object record) {
        String sql = "DELETE FROM " + tableName + " WHERE " + getPrimaryKeyField().name + " = ?;";
        List<Object> parameters = new ArrayList<>();
        try {
            parameters.add(getPrimaryKeyField().getValue(record));
        } catch (Exception e) {
            throw new RuntimeException("Error getting value for primary key field " + getPrimaryKeyField().name, e);
        }
        return new StatementRecord(sql, parameters);
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

record StatementRecord(String sql, List<Object> parameters) {
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

        if (component.isAnnotationPresent(ChildLink.class)) {
            if (List.class.isAssignableFrom(component.getType())) {
                Class<?> genericType = (Class<?>) ((java.lang.reflect.ParameterizedType) component.getGenericType())
                        .getActualTypeArguments()[0];
                if (genericType.isAnnotationPresent(SqlRecord.class)) {
                    return new SqlField(component.getName(), SqlFieldType.CHILD_RECORD, flags,
                            List.of(), List.of(component.getAnnotations()), component);
                } else {
                    throw new RuntimeException("Unsupported generic type for List: " + genericType.getName());
                }
            } else {
                if (component.getType().isAnnotationPresent(SqlRecord.class)) {
                    return new SqlField(component.getName(), SqlFieldType.CHILD_RECORD, flags,
                            List.of(), List.of(component.getAnnotations()), component);
                } else {
                    throw new RuntimeException(
                            "Unsupported type for @ChildLink: " + component.getType().getName());
                }
            }
        }

        if (component.isAnnotationPresent(ParentLink.class)) {
            if (component.getType().isAnnotationPresent(SqlRecord.class)) {
                SqlFieldType type = mapJavaTypeToSqlType(component.getType());

                Class<?> referencedClass = component.getType();
                String referencedTable = referencedClass.getSimpleName();

                return new SqlField(component.getName(), type, flags,
                        List.of(new ForeignKeyConstraint(component.getName(), referencedTable,
                                getPrimaryKeyFieldName(referencedClass))),
                        List.of(component.getAnnotations()), component);
            } else {
                throw new RuntimeException(
                        "Unsupported type for @ParentLink: " + component.getType().getName());
            }
        }
        String name = component.getName();
        SqlFieldType type = mapJavaTypeToSqlType(component.getType());

        List<Constraint> constraints = new ArrayList<>();
        if (component.isAnnotationPresent(PrimaryKey.class)) {
            constraints.add(new PrimaryKeyConstraint(name));
        }

        // if (component.isAnnotationPresent(ForeignKey.class)) {
        // ForeignKey fk = component.getAnnotation(ForeignKey.class);

        // // Validate that the referenced class is annotated with @SqlRecord and is in
        // the
        // // same database
        // Class<?> referencedClass = fk.referencedClass();
        // SqlRecord referencedRecordAnnotation =
        // referencedClass.getAnnotation(SqlRecord.class);
        // if (referencedRecordAnnotation == null) {
        // throw new RuntimeException(
        // "Referenced class " + referencedClass.getName() + " is not annotated with
        // @SqlRecord");
        // }
        // SqlRecord thisRecordAnnotation =
        // component.getDeclaringRecord().getAnnotation(SqlRecord.class);
        // if
        // (!thisRecordAnnotation.dbName().equals(referencedRecordAnnotation.dbName()))
        // {
        // throw new RuntimeException("Referenced class " + referencedClass.getName()
        // + " is in a different database than " +
        // component.getDeclaringRecord().getName());
        // }

        // // Find the primary key field of the referenced class
        // String referencedTable = referencedClass.getSimpleName();
        // String referencedColumn = null;
        // for (RecordComponent rc : referencedClass.getRecordComponents()) {
        // if (rc.isAnnotationPresent(PrimaryKey.class)) {
        // referencedColumn = rc.getName();
        // break;
        // }
        // }
        // constraints.add(new ForeignKeyConstraint(name, referencedTable,
        // referencedColumn));
        // }

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
    CHILD_RECORD("CHILD_RECORD", c -> false),;

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

@FunctionalInterface
interface ValueGetter {
    Object getValue(Object record) throws Exception;
}

// enum SqlFieldFlag {
// NOT_NULL("NOT NULL"),
// UNIQUE("UNIQUE");

// final String sql;

// private SqlFieldFlag(String sql) {
// this.sql = sql;
// }

// String toSql() {
// return sql;
// }
// }

interface Flag {
    String toSql();
}

class NotNullFlag implements Flag {
    @Override
    public String toSql() {
        return "NOT NULL";
    }
}

// class DefaultFlag implements Flag {
// private final SqlFieldType type;
// private final String defaultValue;

// public DefaultFlag(SqlFieldType type, String defaultValue) {
// this.type = type;
// this.defaultValue = defaultValue;
// }

// @Override
// public String toSql() {
// return "DEFAULT " + defaultValue;
// }
// }

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