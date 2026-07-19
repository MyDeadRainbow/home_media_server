package com.hms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class SQLiteRecordDao<T extends SQLiteRecord> {

    public abstract String getDbPath();

    public abstract String getTableName();

    public abstract String toCreateTableStatement();

    public abstract PreparedStatementValue toInsertStatement(T record);

    public abstract PreparedStatementValue toUpdateStatement(T record);

    public abstract PreparedStatementValue toDeleteStatement(T record);

    public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(getTableName());
        if (conditions != null && !conditions.isEmpty()) {
            sb.append(" WHERE ");
            boolean first = true;
            for (String field : conditions.keySet()) {
                if (!first) {
                    sb.append(" AND ");
                }
                sb.append(field).append(" = ?");
                first = false;
            }
        }
        sb.append(";");
        return new PreparedStatementValue(sb.toString(), conditions.values().toArray());
    }

    public abstract T mapResultSetToRecord(ResultSet rs) throws SQLException;

    public abstract String getPrimaryKeyField();

    public abstract Object getPrimaryKeyValue(T record);

    public List<SQLiteRecordDao<?>> getDependecies() {
        return List.of();
    }

    public T get(Object primaryKeyValue)
            throws SQLException {
        List<T> results = select(Map.of(getPrimaryKeyField(), primaryKeyValue));
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    public List<T> select(Map<String, Object> conditions)
            throws SQLException {
        ensureTableExists();
        try (var conn = Database.getConnection(getDbPath())) {
            PreparedStatementValue psValue = toSelectStatement(conditions);
            try (var stmt = conn.prepareStatement(psValue.statement())) {
                for (int i = 0; i < psValue.values().length; i++) {
                    stmt.setObject(i + 1, psValue.values()[i]);
                }
                try (var rs = stmt.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rs.next()) {
                        T rowInstance = mapResultSetToRecord(rs);
                        results.add(rowInstance);
                    }
                    return results;
                }
            }
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    public void insert(T record) throws SQLException {
        ensureTableExists();
        if (get(getPrimaryKeyValue(record)) != null) {
            update(record);
            return;
        }
        PreparedStatementValue psValue = toInsertStatement(record);
        try (var conn = Database.getConnection(getDbPath()); var stmt = conn.prepareStatement(psValue.statement())) {
            for (int i = 0; i < psValue.values().length; i++) {
                stmt.setObject(i + 1, psValue.values()[i]);
            }
            stmt.executeUpdate();
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    public void update(T record) throws SQLException {
        ensureTableExists();
        if (get(getPrimaryKeyValue(record)) == null) {
            insert(record);
            return;
        }
        PreparedStatementValue psValue = toUpdateStatement(record);
        try (var conn = Database.getConnection(getDbPath()); var stmt = conn.prepareStatement(psValue.statement())) {
            for (int i = 0; i < psValue.values().length; i++) {
                stmt.setObject(i + 1, psValue.values()[i]);
            }
            stmt.executeUpdate();
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    public void delete(T record) throws SQLException {
        ensureTableExists();
        PreparedStatementValue psValue = toDeleteStatement(record);
        try (var conn = Database.getConnection(getDbPath()); var stmt = conn.prepareStatement(psValue.statement())) {
            for (int i = 0; i < psValue.values().length; i++) {
                stmt.setObject(i + 1, psValue.values()[i]);
            }
            stmt.executeUpdate();
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }

    public void ensureTableExists() throws SQLException {        
        try (var conn = Database.getConnection(getDbPath());) {
            for (SQLiteRecordDao<?> dao : getDependecies()) {
                dao.ensureTableExists();
            }
            try (var stmt = conn.createStatement()) {                
                stmt.execute(toCreateTableStatement());
            }
        } catch (DBFileNotFoundException | GetConnectionException e) {
            throw new SQLException("Failed to get database connection", e);
        }
    }
}
