package com.hms.stream.importmedia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.media.MediaCategory;

public record ImportMediaEntry(String id, MediaCategory category, String title, ImportMediaStatus status,
        String magnetLink, LocalDateTime createdAt, String torrentFolderPath, String resumeFile, String magnetDataFile)
        implements SQLiteRecord {

    ImportMediaEntry withStatus(ImportMediaStatus newStatus) {
        return new ImportMediaEntry(this.id, this.category, this.title, newStatus, this.magnetLink, this.createdAt,
                this.torrentFolderPath, this.resumeFile, this.magnetDataFile);
    }

    ImportMediaEntry withMagnetLink(String newMagnetLink) {
        return new ImportMediaEntry(this.id, this.category, this.title, this.status, newMagnetLink, this.createdAt,
                this.torrentFolderPath, this.resumeFile, this.magnetDataFile);
    }

    ImportMediaEntry withTorrentFolderPath(String newTorrentFolderPath) {
        return new ImportMediaEntry(this.id, this.category, this.title, this.status, this.magnetLink, this.createdAt,
                newTorrentFolderPath, this.resumeFile, this.magnetDataFile);
    }

    ImportMediaEntry withResumeFile(String newResumeFile) {
        return new ImportMediaEntry(this.id, this.category, this.title, this.status, this.magnetLink, this.createdAt,
                this.torrentFolderPath, newResumeFile, this.magnetDataFile);
    }

    ImportMediaEntry withMagnetDataFile(String newMagnetDataFile) {
        return new ImportMediaEntry(this.id, this.category, this.title, this.status, this.magnetLink, this.createdAt,
                this.torrentFolderPath, this.resumeFile, newMagnetDataFile);
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
                    + "createdAt TEXT NOT NULL,"
                    + "torrentFolderPath TEXT,"
                    + "resumeFile TEXT,"
                    + "magnetDataFile TEXT"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(ImportMediaEntry record) {
            return new PreparedStatementValue(
                    "INSERT INTO import_media_entries (id, category, title, status, magnetLink, createdAt, torrentFolderPath, resumeFile, magnetDataFile) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
                    new Object[] { record.id(), record.category(), record.title(), record.status(), record.magnetLink(),
                            record.createdAt(), record.torrentFolderPath(), record.resumeFile(),
                            record.magnetDataFile() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(ImportMediaEntry record) {
            return new PreparedStatementValue(
                    "UPDATE import_media_entries SET category = ?, title = ?, status = ?, magnetLink = ?, createdAt = ?, torrentFolderPath = ?, resumeFile = ?, magnetDataFile = ? WHERE id = ?;",
                    new Object[] { record.category(), record.title(), record.status(), record.magnetLink(),
                            record.createdAt(), record.torrentFolderPath(), record.resumeFile(),
                            record.magnetDataFile(), record.id() });
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
                    LocalDateTime.parse(rs.getString("createdAt")),
                    rs.getString("torrentFolderPath"),
                    rs.getString("resumeFile"),
                    rs.getString("magnetDataFile"));
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
