package com.hms.catalog.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;

public record Episode(
        String episodeId,
        String seasonId,
        String seriesId,
        MediaItem media,
        String name, int episodeNumber)
        implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "episodeId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return episodeId;
    }

    public static class Dao extends SQLiteRecordDao<Episode> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "episodes";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS episodes ("
                    + "episodeId TEXT PRIMARY KEY,"
                    + "mediaId TEXT NOT NULL,"
                    + "seasonId TEXT NOT NULL,"
                    + "seriesId TEXT NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "episodeNumber INTEGER NOT NULL,"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(mediaId),"
                    + "FOREIGN KEY(seasonId) REFERENCES seasons(seasonId),"
                    + "FOREIGN KEY(seriesId) REFERENCES series(seriesId)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Episode record) {
            return new PreparedStatementValue(
                    "INSERT INTO episodes (episodeId, mediaId, seasonId, seriesId, name, episodeNumber) VALUES (?, ?, ?, ?, ?, ?);",
                    new Object[] { record.episodeId(), record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.name(),
                            record.episodeNumber() });
        }

        @Override
        public void delete(Episode record) throws SQLException {
            new MediaItem.Dao().delete(record.media());
            super.delete(record);
        }

        @Override
        public void insert(Episode record) throws SQLException {
            new MediaItem.Dao().insert(record.media());
            super.insert(record);
        }

        @Override
        public void update(Episode record) throws SQLException {
            new MediaItem.Dao().update(record.media());
            super.update(record);
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Episode record) {
            return new PreparedStatementValue(
                    "UPDATE episodes SET mediaId = ?, seasonId = ?, seriesId = ?, name = ?, episodeNumber = ? WHERE episodeId = ?;",
                    new Object[] { record.media().mediaId(), record.seasonId(), record.seriesId(), record.name(),
                            record.episodeNumber(),
                            record.episodeId() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(Episode record) {
            return new PreparedStatementValue(
                    "DELETE FROM episodes WHERE episodeId = ?;",
                    new Object[] { record.episodeId() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            Preconditions.checkNotNull(conditions, "Conditions map cannot be null");
            StringBuilder query = new StringBuilder("SELECT * FROM episodes");
            if (conditions != null && !conditions.isEmpty()) {
                query.append(" WHERE ");
                conditions.forEach((key, value) -> {
                    query.append(key).append(" = ? AND ");
                });
                // Remove the last " AND "
                query.setLength(query.length() - 5);
            }
            return new PreparedStatementValue(query.toString(), conditions.values().toArray());
        }

        @Override
        public Episode mapResultSetToRecord(ResultSet rs) throws SQLException {
            String episodeId = rs.getString("episodeId");
            String seasonId = rs.getString("seasonId");
            String seriesId = rs.getString("seriesId");
            String name = rs.getString("name");
            int episodeNumber = rs.getInt("episodeNumber");
            String mediaId = rs.getString("mediaId");
            MediaItem media = new MediaItem.Dao().get(mediaId);
            return new Episode(episodeId, seasonId, seriesId, media, name, episodeNumber);
        }

        @Override
        public String getPrimaryKeyField() {
            return "episodeId";
        }

        @Override
        public Object getPrimaryKeyValue(Episode record) {
            return record.episodeId();
        }
    }
}
