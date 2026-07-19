package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.hms.dao.Identity;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;

public record FileName(Identity fileNameId, String fileName, MediaItem mediaItem) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "fileNameId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return fileNameId.id();
    }

    public static class Dao extends SQLiteRecordDao<FileName> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "file_names";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS file_names ("
                    + "fileNameId TEXT PRIMARY KEY,"
                    + "fileName TEXT NOT NULL,"
                    + "mediaId TEXT NOT NULL,"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(id)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(FileName record) {
            return new PreparedStatementValue(
                    "INSERT INTO file_names (fileNameId, fileName, mediaId) VALUES (?, ?, ?);",
                    new Object[] { record.fileNameId(), record.fileName(), record.mediaItem.mediaId() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(FileName record) {
            return new PreparedStatementValue(
                    "UPDATE file_names SET fileName = ?, mediaId = ? WHERE fileNameId = ?;",
                    new Object[] { record.fileName(), record.mediaItem.mediaId(), record.fileNameId().id() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(FileName record) {
            return new PreparedStatementValue(
                    "DELETE FROM file_names WHERE fileNameId = ?;",
                    new Object[] { record.fileNameId().id() });
        }

        @Override
        public FileName mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new FileName(
                    new Identity(rs.getString("fileNameId")),
                    rs.getString("fileName"),
                    new MediaItem.Dao().get(rs.getString("mediaId"))
            );
        }

        @Override
        public String getPrimaryKeyField() {
            return "fileNameId";
        }

        @Override
        public Object getPrimaryKeyValue(FileName record) {
            return record.fileNameId().id();
        }

    }
    
}
