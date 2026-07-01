package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.shared.messaging.JsonSerializable;

public record MediaItem(String mediaId, String filePath) implements SQLiteRecord, JsonSerializable<MediaItem> {

    @Override
    public String getPrimaryKeyField() {
        return "mediaId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return mediaId;
    }

    public static MediaItem create(String filePath) {
        String mediaId = UUID.randomUUID().toString();
        return new MediaItem(mediaId, filePath);
    }

    public MediaItem withMediaId(String newMediaId) {
        return new MediaItem(newMediaId, this.filePath);
    }

    public MediaItem withFilePath(String newFilePath) {
        return new MediaItem(this.mediaId, newFilePath);
    }

    public static class Dao extends com.hms.dao.SQLiteRecordDao<MediaItem> {

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
                    + "mediaId TEXT PRIMARY KEY,"
                    + "filePath TEXT NOT NULL"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(MediaItem record) {
            return new PreparedStatementValue(
                    "INSERT INTO media_items (mediaId, filePath) VALUES (?, ?);",
                    new Object[] { record.mediaId(), record.filePath() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(MediaItem record) {
            return new PreparedStatementValue(
                    "UPDATE media_items SET filePath = ? WHERE mediaId = ?;",
                    new Object[] { record.filePath(), record.mediaId() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(MediaItem record) {
            return new PreparedStatementValue(
                    "DELETE FROM media_items WHERE mediaId = ?;",
                    new Object[] { record.mediaId() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + getTableName() + " WHERE 1=1");
            Object[] params = new Object[conditions.size()];
            int index = 0;
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                queryBuilder.append(" AND ").append(entry.getKey()).append(" = ?");
                params[index++] = entry.getValue();
            }
            return new PreparedStatementValue(queryBuilder.toString(), params);
        }

        @Override
        public MediaItem mapResultSetToRecord(ResultSet rs) throws SQLException {
            String mediaId = rs.getString("mediaId");
            String filePath = rs.getString("filePath");
            return new MediaItem(mediaId, filePath);
        }

        @Override
        public String getPrimaryKeyField() {
            return "mediaId";
        }

        @Override
        public Object getPrimaryKeyValue(MediaItem record) {
            return record.mediaId();
        }
    }
}
