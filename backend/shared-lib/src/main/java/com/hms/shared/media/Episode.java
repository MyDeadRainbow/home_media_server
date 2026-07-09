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
import com.hms.shared.media.poster.Poster;
import com.hms.shared.messaging.JsonSerializable;

public record Episode(
        String episodeId,
        String seasonId,
        String seriesId,
        int episodeNumber,
        MediaItem media,
        MetaData metaData,
        Poster poster)
        implements SQLiteRecord, JsonSerializable, Title {

    @Override
    public String getPrimaryKeyField() {
        return "episodeId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return episodeId;
    }

    public static Episode create(String seasonId, String seriesId, int episodeNumber, MediaItem media,
            MetaData metaData, Poster poster) {
        String episodeId = UUID.randomUUID().toString();
        return new Episode(episodeId, seasonId, seriesId, episodeNumber, media, metaData, poster);
    }

    @Override
    public String title() {
        return metaData.title();
    }

    public Episode withEpisodeId(String newEpisodeId) {
        return new Episode(newEpisodeId, this.seasonId, this.seriesId, this.episodeNumber, this.media,
                this.metaData, this.poster);
    }

    public Episode withSeasonId(String newSeasonId) {
        return new Episode(this.episodeId, newSeasonId, this.seriesId, this.episodeNumber, this.media,
                this.metaData, this.poster);
    }

    public Episode withSeriesId(String newSeriesId) {
        return new Episode(this.episodeId, this.seasonId, newSeriesId, this.episodeNumber, this.media,
                this.metaData, this.poster);
    }

    public Episode withEpisodeNumber(int newEpisodeNumber) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, newEpisodeNumber, this.media,
                this.metaData, this.poster);
    }

    public Episode withMedia(MediaItem newMedia) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.episodeNumber, newMedia,
                this.metaData, this.poster);
    }

    public Episode withMetaData(MetaData newMetaData) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.episodeNumber, this.media,
                newMetaData, this.poster);
    }

    public Episode withPoster(Poster newPoster) {
        return new Episode(this.episodeId, this.seasonId, this.seriesId, this.episodeNumber, this.media,
                this.metaData, newPoster);
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
                    + "posterId TEXT NOT NULL,"
                    + "episodeNumber INTEGER NOT NULL,"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(mediaId),"
                    + "FOREIGN KEY(seasonId) REFERENCES seasons(seasonId),"
                    + "FOREIGN KEY(seriesId) REFERENCES series(seriesId),"
                    + "FOREIGN KEY(metaDataId) REFERENCES metadata(metaDataId),"
                    + "FOREIGN KEY(posterId) REFERENCES posters(posterId)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Episode record) {
            return new PreparedStatementValue(
                    "INSERT INTO episodes (episodeId, mediaId, seasonId, seriesId, metaDataId, episodeNumber, posterId) VALUES (?, ?, ?, ?, ?, ?, ?);",
                    new Object[] { record.episodeId(), record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.metaData().metaDataId(),
                            record.episodeNumber(),
                            record.poster().posterId() });
        }

        @Override
        public void delete(Episode record) throws SQLException {
            new MediaItem.Dao().delete(record.media());
            new MetaData.Dao().delete(record.metaData());
            new Poster.Dao().delete(record.poster());
            super.delete(record);
        }

        @Override
        public void insert(Episode record) throws SQLException {
            new MediaItem.Dao().insert(record.media());
            new MetaData.Dao().insert(record.metaData());
            new Poster.Dao().insert(record.poster());
            super.insert(record);
        }

        @Override
        public void update(Episode record) throws SQLException {
            new MediaItem.Dao().update(record.media());
            new MetaData.Dao().update(record.metaData());
            new Poster.Dao().update(record.poster());
            super.update(record);
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Episode record) {
            return new PreparedStatementValue(
                    "UPDATE episodes SET mediaId = ?, seasonId = ?, seriesId = ?, metaDataId = ?, episodeNumber = ?, posterId = ? WHERE episodeId = ?;",
                    new Object[] { record.media().mediaId(), record.seasonId(), record.seriesId(),
                            record.metaData().metaDataId(),
                            record.episodeNumber(),
                            record.poster().posterId(),
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
            String posterId = rs.getString("posterId");
            Poster poster = new Poster.Dao().get(posterId);
            return new Episode(episodeId, seasonId, seriesId, episodeNumber, media, metaData, poster);
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
