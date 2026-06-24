package com.hms.stream.importmedia;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.media.MediaCategory;

public record ImportMediaEntry(String id, MediaCategory category, String title, ImportMediaStatus status,
        String magnetLink, Date createdAt, String torrentFolderPath)
        implements SQLiteRecord {

    ImportMediaEntry withStatus(ImportMediaStatus newStatus) {
        return new ImportMediaEntry(this.id, this.category, this.title, newStatus, this.magnetLink, this.createdAt,
                this.torrentFolderPath);
    }

    ImportMediaEntry withMagnetLink(String newMagnetLink) {
        return new ImportMediaEntry(this.id, this.category, this.title, this.status, newMagnetLink, this.createdAt,
                this.torrentFolderPath);
    }

    ImportMediaEntry withTorrentFolderPath(String newTorrentFolderPath) {
        return new ImportMediaEntry(this.id, this.category, this.title, this.status, this.magnetLink, this.createdAt,
                newTorrentFolderPath);
    }

    @Override
    public String getPrimaryKeyField() {
        return "id";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return this.id;
    }

    public static class Dao extends SQLiteRecordDao<ImportMediaEntry> {

        @Override
        public String getDbPath() {
            return "import_media.db";
        }

        @Override
        public String getTableName() {
            return "import_media_entries";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS import_media_entries ("
                    + "id TEXT PRIMARY KEY,"
                    + "category TEXT NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "status TEXT NOT NULL,"
                    + "magnetLink TEXT,"
                    + "createdAt DATE NOT NULL,"
                    + "torrentFolderPath TEXT"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(ImportMediaEntry record) {
            return new PreparedStatementValue(
                    "INSERT INTO import_media_entries (id, category, title, status, magnetLink, createdAt, torrentFolderPath) VALUES (?, ?, ?, ?, ?, ?, ?);",
                    new Object[] { record.id(), record.category(), record.title(), record.status(), record.magnetLink(),
                            record.createdAt(), record.torrentFolderPath() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(ImportMediaEntry record) {
            return new PreparedStatementValue(
                    "UPDATE import_media_entries SET category = ?, title = ?, status = ?, magnetLink = ?, createdAt = ?, torrentFolderPath = ? WHERE id = ?;",
                    new Object[] { record.category(), record.title(), record.status(), record.magnetLink(),
                            record.createdAt(), record.torrentFolderPath(), record.id() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(ImportMediaEntry record) {
            return new PreparedStatementValue(
                    "DELETE FROM import_media_entries WHERE id = ?;",
                    new Object[] { record.id() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            Preconditions.checkNotNull(conditions, "Conditions map cannot be null");
            StringBuilder query = new StringBuilder("SELECT * FROM import_media_entries");
            if (conditions != null && !conditions.isEmpty()) {
                query.append(" WHERE ");
                boolean first = true;
                for (String key : conditions.keySet()) {
                    if (!first) {
                        query.append(" AND ");
                    }
                    query.append(key).append(" = ?");
                    first = false;
                }
            }
            return new PreparedStatementValue(query.toString(), conditions.values().toArray());
        }

        @Override
        public ImportMediaEntry mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new ImportMediaEntry(
                    rs.getString("id"),
                    MediaCategory.valueOf(rs.getString("category")),
                    rs.getString("title"),
                    ImportMediaStatus.valueOf(rs.getString("status")),
                    rs.getString("magnetLink"),
                    rs.getDate("createdAt"),
                    rs.getString("torrentFolderPath")
            );
        }

        @Override
        public String getPrimaryKeyField() {
            return "id";
        }

        @Override
        public Object getPrimaryKeyValue(ImportMediaEntry record) {
            return record.id();
        }
    }
}
