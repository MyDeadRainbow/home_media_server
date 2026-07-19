package com.hms.stream.importmedia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.media.MediaItem;

public record ImportMediaEntryMediaItem(String id, String mediaId, String importMediaEntryId) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "id";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return id;
    }

    public static class Dao extends SQLiteRecordDao<ImportMediaEntryMediaItem> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "import_media_entry_media_item";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS import_media_entry_media_item ("
                    + "id TEXT PRIMARY KEY,"
                    + "mediaId TEXT NOT NULL,"
                    + "importMediaEntryId TEXT NOT NULL,"
                    + "FOREIGN KEY(importMediaEntryId) REFERENCES import_media_entries(id),"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(id)"
                    + ");";
        }

        @Override
        public List<SQLiteRecordDao<?>> getDependecies() {
            return List.of(new ImportMediaEntry.Dao(), new MediaItem.Dao());
        }

        @Override
        public PreparedStatementValue toInsertStatement(ImportMediaEntryMediaItem record) {
            return new PreparedStatementValue(
                    "INSERT INTO import_media_entry_media_item (id, mediaId, importMediaEntryId) VALUES (?, ?, ?);",
                    new Object[] { record.id(), record.mediaId(), record.importMediaEntryId() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(ImportMediaEntryMediaItem record) {
            return new PreparedStatementValue(
                    "UPDATE import_media_entry_media_item SET mediaId = ?, importMediaEntryId = ? WHERE id = ?;",
                    new Object[] { record.mediaId(), record.importMediaEntryId(), record.id() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(ImportMediaEntryMediaItem record) {
            return new PreparedStatementValue(
                    "DELETE FROM import_media_entry_media_item WHERE id = ?;",
                    new Object[] { record.id() });
        }

        @Override
        public ImportMediaEntryMediaItem mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new ImportMediaEntryMediaItem(
                    rs.getString("id"),
                    rs.getString("mediaId"),
                    rs.getString("importMediaEntryId"));
        }

        @Override
        public String getPrimaryKeyField() {
            return "id";
        }

        @Override
        public Object getPrimaryKeyValue(ImportMediaEntryMediaItem record) {
            return record.id();
        }
    }
}
