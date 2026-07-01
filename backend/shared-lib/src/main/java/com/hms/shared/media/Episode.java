package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.messaging.JsonSerializable;

public record Episode(
        String episodeId,
        String seasonId,
        String seriesId,
        int episodeNumber,
        MediaItem media,
        MetaData metaData)
        implements SQLiteRecord, JsonSerializable<Episode>, Title {

    @Override
    public String getPrimaryKeyField() {
        return "episodeId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return episodeId;
    }

    public static Episode create(String seasonId, String seriesId, int episodeNumber, MediaItem media,
            MetaData metaData) {
        String episodeId = UUID.randomUUID().toString();
        return new Episode(episodeId, seasonId, seriesId, episodeNumber, media, metaData);
    }

    @Override
    public String title() {
        return metaData.title();
    }

    public Episode withEpisodeId(String newEpisodeId) {
        return new Episode(newEpisodeId, this.seasonId, this.seriesId, this.episodeNumber, this.media,
                this.metaData);
    }

    public Episode withSeasonId(String newSeasonId) {
        return new Episode(this.episodeId, newSeasonId, this.seriesId, this.episodeNumber, this.media,
                this.metaData);
    }

    public Episode withSeriesId(String newSeriesId) {
        return new Episode(this.episodeId, this.seasonId, newSeriesId, this.episodeNumber, this.media,
                this.metaData);
    }

    public Episode withEpisodeNumber(int newEpisodeNumber) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, newEpisodeNumber, this.media,
                this.metaData);
    }

    public Episode withMedia(MediaItem newMedia) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.episodeNumber, newMedia,
                this.metaData);
    }

    public Episode withMetaData(MetaData newMetaData) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.episodeNumber, this.media,
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
                    "INSERT INTO episodes (episodeId, mediaId, seasonId, seriesId, metaDataId, episodeNumber) VALUES (?, ?, ?, ?, ?, ?);",
                    new Object[] { record.episodeId(), record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.metaData().metaDataId(),
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
                    "UPDATE episodes SET mediaId = ?, seasonId = ?, seriesId = ?, metaDataId = ?, episodeNumber = ? WHERE episodeId = ?;",
                    new Object[] { record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.metaData().metaDataId(),
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
            int episodeNumber = rs.getInt("episodeNumber");
            String mediaId = rs.getString("mediaId");
            String metaDataId = rs.getString("metaDataId");
            MediaItem media = new MediaItem.Dao().get(mediaId);
            MetaData metaData = new MetaData.Dao().get(metaDataId);
            return new Episode(episodeId, seasonId, seriesId, episodeNumber, media, metaData);
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
