package com.hms.catalog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.hms.shared.dao.PreparedStatementValue;
import com.hms.shared.dao.SQLiteRecord;

public record MediaItem(String id, String title, String type, Integer year, String description,
        String posterUrl, String streamUrl) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "id";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return id;
    }

    public static class Dao extends com.hms.shared.dao.SQLiteRecordDao<MediaItem> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "media_items";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS media_items ("
                    + "id TEXT PRIMARY KEY,"
                    + "title TEXT NOT NULL,"
                    + "type TEXT NOT NULL,"
                    + "year INTEGER,"
                    + "description TEXT,"
                    + "posterUrl TEXT,"
                    + "streamUrl TEXT"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(MediaItem record) {
            return new PreparedStatementValue(
                    "INSERT INTO media_items (id, title, type, year, description, posterUrl, streamUrl) VALUES (?, ?, ?, ?, ?, ?, ?);",
                    new Object[] { record.id(), record.title(), record.type(), record.year(),
                            record.description(), record.posterUrl(), record.streamUrl() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(MediaItem record) {
            return new PreparedStatementValue(
                    "UPDATE media_items SET title = ?, type = ?, year = ?, description = ?, posterUrl = ?, streamUrl = ? WHERE id = ?;",
                    new Object[] { record.title(), record.type(), record.year(),
                            record.description(), record.posterUrl(), record.streamUrl(),
                            record.id() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(MediaItem record) {
            return new PreparedStatementValue(
                    "DELETE FROM media_items WHERE id = ?;",
                    new Object[] { record.id() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            Preconditions.checkNotNull(conditions, "Conditions map cannot be null");
            StringBuilder query = new StringBuilder("SELECT * FROM media_items");
            if (conditions != null && !conditions.isEmpty()) {
                query.append(" WHERE ");
                conditions.forEach((key, value) -> {
                    query.append(key).append(" = ? AND ");
                });
                // Remove the last " AND "
                query.setLength(query.length() - 5);
            }
            query.append(";");

            return new PreparedStatementValue(query.toString(), conditions.values().toArray());
        }

        @Override
        public MediaItem mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new MediaItem(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("type"),
                    rs.getInt("year"),
                    rs.getString("description"),
                    rs.getString("posterUrl"),
                    rs.getString("streamUrl"));
        }

        @Override
        public String getPrimaryKeyField() {
            return "id";
        }

        @Override
        public Object getPrimaryKeyValue(MediaItem record) {
            return record.id();
        }
    }
}
