package com.hms.annotated.sql.view;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hms.annotated.sql.ManyToOne;
import com.hms.annotated.sql.OneToMany;
import com.hms.annotated.sql.OneToOne;
import com.hms.annotated.sql.PrimaryKey;
import com.hms.annotated.sql.SqlRecordDao;
import com.hms.annotated.sql.StatementValues;
import com.hms.annotated.sql.TableRecord;
import com.hms.annotated.sql.ViewRecord;
import com.hms.dao.Database;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class ViewRecordDao<T> {
    private static final Map<Class<?>, ViewRecordDao<?>> factories = new HashMap<>();

    static {
        // get all classes annotated with @SqlRecord and create factories for them
        try {
            ClassGraph classGraph = new ClassGraph().enableAllInfo().acceptPackages("com.hms.*");
            try (ScanResult scanResult = classGraph.scan()) {
                List<ClassInfo> sqlRecordClasses = scanResult.getClassesWithAnnotation(ViewRecord.class.getName());
                for (ClassInfo classInfo : sqlRecordClasses) {
                    Class<?> clazz = classInfo.loadClass();
                    factories.put(clazz, new ViewRecordDao<>(clazz));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing ViewRecordDao", e);
        }
    }

    public static <T> ViewRecordDao<T> getFactory(Class<T> recordClass) {
        @SuppressWarnings("unchecked")
        ViewRecordDao<T> factory = (ViewRecordDao<T>) factories.get(recordClass);
        if (factory == null) {
            throw new RuntimeException("No factory found for class: " + recordClass.getName());
        }
        return factory;
    }

    private final String dbName;
    private final Class<T> recordClass;
    private final RecordComponent[] fields;
    private final Constructor<T> constructor;

    private ViewRecordDao(Class<T> recordClass) throws Exception {
        this.recordClass = recordClass;
        ViewRecord viewRecordAnnotation = recordClass.getAnnotation(ViewRecord.class);
        if (viewRecordAnnotation == null) {
            throw new IllegalArgumentException(
                    "Class " + recordClass.getName() + " is not annotated with @ViewRecord");
        }
        this.dbName = viewRecordAnnotation.dbName();
        this.fields = recordClass.getRecordComponents();
        this.constructor = recordClass.getDeclaredConstructor(getParameters(recordClass));

        // Create the view in the database if it doesn't exist
        try (var connection = Database.getConnection(dbName);
                var statement = connection.createStatement()) {
            statement.execute(toDropStatement());
            statement.execute(toCreateStatement());
        } catch (Exception e) {
            throw new RuntimeException("Error creating view for " + recordClass.getName(), e);
        }
    }

    private static Class<?>[] getParameters(Class<?> recordClass) {
        return recordClass.getRecordComponents() != null
                ? java.util.Arrays.stream(recordClass.getRecordComponents())
                        .map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)
                : new Class<?>[0];
    }

    private String toDropStatement() {
        return "DROP VIEW IF EXISTS " + recordClass.getSimpleName();
    }

    private String toCreateStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE VIEW ").append(recordClass.getSimpleName()).append(" AS SELECT \n");
        StringBuilder fromSb = new StringBuilder();
        StringBuilder fieldsSb = new StringBuilder();
        StringBuilder joinsSb = new StringBuilder();
        for (RecordComponent field : fields) {
            Class<?> fieldType = field.getType();
            fieldsSb.append(fieldType.getSimpleName()).append('.').append(getPrimaryKeyField(fieldType).getName())
                    .append(", \n");

            if (field.isAnnotationPresent(PrimaryJoin.class)) {
                fromSb.append("FROM ").append(fieldType.getSimpleName()).append(" \n");
            } else if (field.isAnnotationPresent(InnerJoin.class)) {
                InnerJoin innerJoin = field.getAnnotation(InnerJoin.class);
                Class<?> referencedClass = innerJoin.referencedClass();
                joinsSb.append("INNER JOIN ").append(fieldType.getSimpleName()).append(" ON ")
                        .append(fieldType.getSimpleName()).append('.')
                        .append(getPrimaryKeyField(referencedClass).getName())
                        .append(" = ").append(referencedClass.getSimpleName()).append('.')
                        .append(getPrimaryKeyField(referencedClass).getName()).append(" \n");
            } else if (field.isAnnotationPresent(LeftJoin.class)) {
                LeftJoin leftJoin = field.getAnnotation(LeftJoin.class);
                Class<?> referencedClass = leftJoin.referencedClass();

                // generic type for lists needed here
                RecordComponent mainFieldReferenceField = Arrays.stream(fieldType.getRecordComponents())
                        .filter(rc -> {
                            boolean result = rc.getType().equals(referencedClass);
                            if (!result) {
                                if (List.class.isAssignableFrom(rc.getType())) {
                                    result = referencedClass.equals(
                                            ((ParameterizedType) rc.getGenericType()).getActualTypeArguments()[0]);
                                } else {
                                    result = false;
                                }
                            }
                            return result;
                        })
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("No field named " + referencedClass.getSimpleName()
                                + " found in " + fieldType.getName()));

                String leftFieldName = null;
                String rightFieldName = null;

                if (mainFieldReferenceField.isAnnotationPresent(OneToOne.class)) {
                    leftFieldName = fieldType.getSimpleName() + "."
                            + getPrimaryKeyField(mainFieldReferenceField.getType()).getName();
                    rightFieldName = mainFieldReferenceField.getType().getSimpleName() + "."
                            + getPrimaryKeyField(mainFieldReferenceField.getType());
                } else if (mainFieldReferenceField.isAnnotationPresent(ManyToOne.class)) {
                    // leftFieldName = mainFieldReferenceField.getType().getSimpleName() + "."
                    // + getPrimaryKeyField(mainFieldReferenceField.getType()).getName();
                } else if (mainFieldReferenceField.isAnnotationPresent(OneToMany.class)) {
                    leftFieldName = fieldType.getSimpleName() + "." + getPrimaryKeyField(fieldType);
                    rightFieldName = mainFieldReferenceField.getGenericType().getClass().getSimpleName() + "."
                            + getPrimaryKeyField(fieldType).getName();
                } else {
                    throw new RuntimeException("Field " + mainFieldReferenceField.getName() + " in "
                            + fieldType.getName() + " is not annotated with @OneToOne or @ManyToOne");
                }

                joinsSb.append("LEFT JOIN ").append(fieldType.getSimpleName()).append(" ON ")
                        .append(leftFieldName).append(" = ").append(rightFieldName).append(" \n");
            } else if (field.isAnnotationPresent(CustomField.class)) {
                CustomField customField = field.getAnnotation(CustomField.class);
            }
        }
        fieldsSb.setLength(fieldsSb.length() - 3); // Remove the last comma and space and newline
        fieldsSb.append(" \n").append(fromSb);
        sb.append(fieldsSb);
        sb.append(joinsSb);
        return sb.toString();
    }

    private StatementValues toSelectStatement(Map<String, Object> whereConditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(recordClass.getSimpleName());
        if (whereConditions != null && !whereConditions.isEmpty()) {
            sb.append(" WHERE ");
            whereConditions.forEach((key, value) -> {
                sb.append(key).append(" = ? AND ");
            });
            sb.setLength(sb.length() - 5); // Remove the last " AND "
        }
        return new StatementValues(sb.toString(), new ArrayList<>(whereConditions.values()));
    }

    private RecordComponent getPrimaryKeyField(Class<?> recordClass) {
        for (RecordComponent field : recordClass.getRecordComponents()) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                return field;
            }
        }
        throw new RuntimeException("No primary key field found for " + recordClass.getName());
    }

    private Object[] getFieldValues(ResultSet resultSet) throws Exception {
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            RecordComponent field = fields[i];
            Class<?> fieldType = field.getType();
            RecordComponent primaryKeyField = getPrimaryKeyField(fieldType);
            String columnName = fieldType.getSimpleName() + "." + primaryKeyField.getName();
            Object value = resultSet.getObject(columnName);
            SqlRecordDao<?> dao = SqlRecordDao.getFactory(fieldType);
            Object record = dao.select(Map.of(primaryKeyField.getName(), value)).stream().findFirst().orElse(null);
            values[i] = record;
        }
        return values;
    }

    public List<T> select(Map<String, Object> whereConditions) {
        StatementValues statementValues = toSelectStatement(whereConditions);
        try (var connection = Database.getConnection(dbName);
                var preparedStatement = connection.prepareStatement(statementValues.sql())) {
            List<Object> parameters = statementValues.parameters();
            for (int i = 0; i < parameters.size(); i++) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }
            var resultSet = preparedStatement.executeQuery();
            List<T> results = new ArrayList<>();
            while (resultSet.next()) {
                T record = constructor.newInstance(getFieldValues(resultSet));
                results.add(record);
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Error executing select statement", e);
        }
    }
}
