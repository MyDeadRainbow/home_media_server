package com.hms.shared.media.poster;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.messaging.JsonSerializable;

public record Poster(String posterId, byte[] imageData) implements JsonSerializable, SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "posterId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return posterId;
    }

    public static Poster create(byte[] imageData) {
        String posterId = java.util.UUID.randomUUID().toString();
        return new Poster(posterId, imageData);
    }

    public Poster withImageData(byte[] newImageData) {
        return new Poster(this.posterId, newImageData);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Poster other))
            return false;
        if (!Arrays.equals(this.imageData, other.imageData))
            return false;
        return posterId.equals(other.posterId);
    }

    public static class Dao extends SQLiteRecordDao<Poster> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "posters";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS posters ("
                    + "posterId TEXT PRIMARY KEY,"
                    + "imageData BLOB NOT NULL"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Poster record) {
            return new PreparedStatementValue(
                    "INSERT INTO posters (posterId, imageData) VALUES (?, ?);",
                    new Object[] { record.posterId(), record.imageData() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Poster record) {
            return new PreparedStatementValue(
                    "UPDATE posters SET imageData = ? WHERE posterId = ?;",
                    new Object[] { record.imageData(), record.posterId() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(Poster record) {
            return new PreparedStatementValue(
                    "DELETE FROM posters WHERE posterId = ?;",
                    new Object[] { record.posterId() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM posters");
            if (!conditions.isEmpty()) {
                queryBuilder.append(" WHERE ");
                boolean firstCondition = true;
                for (String field : conditions.keySet()) {
                    if (!firstCondition) {
                        queryBuilder.append(" AND ");
                    }
                    queryBuilder.append(field).append(" = ?");
                    firstCondition = false;
                }
            }
            queryBuilder.append(";");
            return new PreparedStatementValue(queryBuilder.toString(), conditions.values().toArray());
        }

        @Override
        public Poster mapResultSetToRecord(ResultSet rs) throws SQLException {
            String posterId = rs.getString("posterId");
            byte[] imageData = rs.getBytes("imageData");
            return new Poster(posterId, imageData);
        }

        @Override
        public String getPrimaryKeyField() {
            return "posterId";
        }

        @Override
        public Object getPrimaryKeyValue(Poster record) {
            return record.posterId();
        }
    }
}
