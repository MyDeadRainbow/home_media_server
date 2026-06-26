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
        String name,
        int episodeNumber,
        MediaItem media,
        MetaData metaData)
        implements SQLiteRecord {    

    @Override
    public String getPrimaryKeyField() {
        return "episodeId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return episodeId;
    }

    public Episode withName(String newName) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, newName, this.episodeNumber, this.media,
                this.metaData);
    }

    public Episode withEpisodeNumber(int newEpisodeNumber) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.name, newEpisodeNumber, this.media,
                this.metaData);
    }

    public Episode withMedia(MediaItem newMedia) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.name, this.episodeNumber, newMedia,
                this.metaData);
    }

    public Episode withMetaData(MetaData newMetaData) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.name, this.episodeNumber, this.media,
                newMetaData);
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
                    + "metaDataId TEXT NOT NULL,"
                    + "name TEXT NOT NULL,"
                    + "episodeNumber INTEGER NOT NULL,"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(mediaId),"
                    + "FOREIGN KEY(seasonId) REFERENCES seasons(seasonId),"
                    + "FOREIGN KEY(seriesId) REFERENCES series(seriesId),"
                    + "FOREIGN KEY(metaDataId) REFERENCES metadata(metaDataId)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Episode record) {
            return new PreparedStatementValue(
                    "INSERT INTO episodes (episodeId, mediaId, seasonId, seriesId, metaDataId, name, episodeNumber) VALUES (?, ?, ?, ?, ?, ?, ?);",
                    new Object[] { record.episodeId(), record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.metaData().metaDataId(), record.name(),
                            record.episodeNumber() });
        }

        @Override
        public void delete(Episode record) throws SQLException {
            new MediaItem.Dao().delete(record.media());
            new MetaData.Dao().delete(record.metaData());
            super.delete(record);
        }

        @Override
        public void insert(Episode record) throws SQLException {
            new MediaItem.Dao().insert(record.media());
            new MetaData.Dao().insert(record.metaData());
            super.insert(record);
        }

        @Override
        public void update(Episode record) throws SQLException {
            new MediaItem.Dao().update(record.media());
            new MetaData.Dao().update(record.metaData());
            super.update(record);
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Episode record) {
            return new PreparedStatementValue(
                    "UPDATE episodes SET mediaId = ?, seasonId = ?, seriesId = ?, metaDataId = ?, name = ?, episodeNumber = ? WHERE episodeId = ?;",
                    new Object[] { record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.metaData().metaDataId(), record.name(),
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
            String metaDataId = rs.getString("metaDataId");
            MediaItem media = new MediaItem.Dao().get(mediaId);
            MetaData metaData = new MetaData.Dao().get(metaDataId);
            return new Episode(episodeId, seasonId, seriesId, name, episodeNumber, media, metaData);
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
