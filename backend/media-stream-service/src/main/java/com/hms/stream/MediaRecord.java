package com.hms.stream;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hms.shared.dao.PreparedStatementValue;
import com.hms.shared.dao.SQLiteRecord;
import com.hms.shared.dao.SQLiteRecordDao;
import com.hms.shared.messaging.JsonSerializable;

public record MediaRecord(String mediaId, String filePath)
        implements JsonSerializable<MediaRecord>, SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "mediaId";
    }

        @Override
    public Object getPrimaryKeyValue() {
        return mediaId;
    }

    public static class Dao extends SQLiteRecordDao<MediaRecord> {

        @Override
        public String getDbPath() {
            return "media.db";
        }

        @Override
        public String getTableName() {
            return "media_records";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS media_records ("
                    + "mediaId TEXT PRIMARY KEY,"
                    + "filePath TEXT NOT NULL"
                    + ");";
        }


        @Override
        public PreparedStatementValue toInsertStatement(MediaRecord record) {
            return new PreparedStatementValue(
                    "INSERT INTO media_records (mediaId, filePath) VALUES (?, ?);",
                    new Object[]{record.mediaId(), record.filePath()}
            );
        }

        @Override
        public PreparedStatementValue toUpdateStatement(MediaRecord record) {
            return new PreparedStatementValue(
                    "UPDATE media_records SET filePath = ? WHERE mediaId = ?;",
                    new Object[]{record.filePath(), record.mediaId()}
            );
        }

        @Override
        public PreparedStatementValue toDeleteStatement(MediaRecord record) {
            return new PreparedStatementValue(
                    "DELETE FROM media_records WHERE mediaId = ?;",
                    new Object[]{record.mediaId()}
            );
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            StringBuilder query = new StringBuilder("SELECT * FROM media_records");
            List<Object> params = new ArrayList<>();

            if (!conditions.isEmpty()) {
                query.append(" WHERE ");
                conditions.forEach((key, value) -> {
                    query.append(key).append(" = ? AND ");
                    params.add(value);
                });
                // Remove the last " AND "
                query.setLength(query.length() - 5);
            }

            return new PreparedStatementValue(query.toString(), params.toArray());
        }

        @Override
        public MediaRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new MediaRecord(
                    rs.getString("mediaId"),
                    rs.getString("filePath")
            );
        }

        @Override
        public String getPrimaryKeyField() {
            return "mediaId";
        }

        @Override
        public Object getPrimaryKeyValue(MediaRecord record) {
            return record.mediaId();
        }

        
    }
}
