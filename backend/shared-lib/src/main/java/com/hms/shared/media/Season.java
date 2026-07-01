package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.messaging.JsonSerializable;

public record Season(String seasonId, String seriesId, int seasonNumber, MetaData metaData, List<Episode> episodes)
        implements SQLiteRecord, JsonSerializable<Season>, Title {

    @Override
    public String getPrimaryKeyField() {
        return "seasonId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return seasonId;
    }

    public static Season create(String seriesId, int seasonNumber, MetaData metaData, List<Episode> episodes) {
        String seasonId = UUID.randomUUID().toString();
        return new Season(seasonId, seriesId, seasonNumber, metaData, episodes);
    }

    @Override
    public String title() {
        return metaData.title();
    }

    public Season withSeasonId(String newSeasonId) {
        return new Season(newSeasonId, this.seriesId, this.seasonNumber, this.metaData, this.episodes);
    }

    public Season withSeriesId(String newSeriesId) {
        return new Season(this.seasonId, newSeriesId, this.seasonNumber, this.metaData, this.episodes);
    }

    public Season withSeasonNumber(int newSeasonNumber) {
        return new Season(this.seasonId, this.seriesId, newSeasonNumber, this.metaData, this.episodes);
    }

    public Season withMetaData(MetaData newMetaData) {
        return new Season(this.seasonId, this.seriesId, this.seasonNumber, newMetaData, this.episodes);
    }

    public Season withEpisodes(List<Episode> newEpisodes) {
        return new Season(this.seasonId, this.seriesId, this.seasonNumber, this.metaData, newEpisodes);
    }

    public Season replaceEpisode(Episode newEpisode) {
        Preconditions.checkArgument(newEpisode.seasonId().equals(this.seasonId),
                "Episode seasonId must match Season seasonId");
        List<Episode> updatedEpisodes = new ArrayList<>(List.copyOf(this.episodes));
        updatedEpisodes.removeIf(episode -> episode.episodeId().equals(newEpisode.episodeId()));
        updatedEpisodes.add(newEpisode);
        return new Season(this.seasonId, this.seriesId, this.seasonNumber, this.metaData, updatedEpisodes);
    }

    public Season addEpisode(Episode newEpisode) {
        Preconditions.checkArgument(newEpisode.seasonId().equals(this.seasonId),
                "Episode seasonId must match Season seasonId");
        List<Episode> updatedEpisodes = new ArrayList<>(List.copyOf(this.episodes));
        updatedEpisodes.removeIf(episode -> episode.episodeId().equals(newEpisode.episodeId()));
        updatedEpisodes.add(newEpisode);
        return new Season(this.seasonId, this.seriesId, this.seasonNumber, this.metaData, updatedEpisodes);
    }

    public Season removeEpisode(Episode episodeToRemove) {
        List<Episode> updatedEpisodes = new ArrayList<>(List.copyOf(this.episodes));
        updatedEpisodes.removeIf(episode -> episode.episodeId().equals(episodeToRemove.episodeId()));
        return new Season(this.seasonId, this.seriesId, this.seasonNumber, this.metaData, updatedEpisodes);
    }

    public static class Dao extends SQLiteRecordDao<Season> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "seasons";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS seasons ("
                    + "seasonId TEXT PRIMARY KEY,"
                    + "seriesId TEXT NOT NULL,"
                    + "seasonNumber INTEGER NOT NULL,"
                    + "metaDataId TEXT,"
                    + "FOREIGN KEY(seriesId) REFERENCES series(seriesId),"
                    + "FOREIGN KEY(metaDataId) REFERENCES metaData(metaDataId)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Season record) {
            return new PreparedStatementValue(
                    "INSERT INTO seasons (seasonId, seriesId, seasonNumber, metaDataId) VALUES (?, ?, ?, ?);",
                    new Object[] { record.seasonId(), record.seriesId(), record.seasonNumber(), record.metaData().metaDataId() });
        }

        @Override
        public void insert(Season record) throws SQLException {
            super.insert(record);
            for (Episode episode : record.episodes()) {
                new Episode.Dao().insert(episode);
            }
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Season record) {
            return new PreparedStatementValue(
                    "UPDATE seasons SET seriesId = ?, seasonNumber = ?, metaDataId = ? WHERE seasonId = ?;",
                    new Object[] { record.seriesId(), record.seasonNumber(), record.metaData().metaDataId(), record.seasonId() });
        }

        @Override
        public void update(Season record) throws SQLException {
            super.update(record);
            for (Episode episode : record.episodes()) {
                new Episode.Dao().update(episode);
            }
        }

        @Override
        public PreparedStatementValue toDeleteStatement(Season record) {
            return new PreparedStatementValue(
                    "DELETE FROM seasons WHERE seasonId = ?;",
                    new Object[] { record.seasonId() });
        }

        @Override
        public void delete(Season record) throws SQLException {
            for (Episode episode : record.episodes()) {
                new Episode.Dao().delete(episode);
            }
            super.delete(record);
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            Preconditions.checkNotNull(conditions, "Conditions map cannot be null");
            StringBuilder query = new StringBuilder("SELECT * FROM seasons");
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
        public Season mapResultSetToRecord(ResultSet rs) throws SQLException {
            String seasonId = rs.getString("seasonId");
            String seriesId = rs.getString("seriesId");
            int seasonNumber = rs.getInt("seasonNumber");
            MetaData metaData = new MetaData.Dao().get(rs.getString("metaDataId")); // Assuming MetaData has a constructor that takes a String
            List<Episode> episodes = new Episode.Dao().select(Map.of("seasonId", seasonId));
            return new Season(seasonId, seriesId, seasonNumber, metaData, episodes);
        }

        @Override
        public String getPrimaryKeyField() {
            return "seasonId";
        }

        @Override
        public Object getPrimaryKeyValue(Season record) {
            return record.seasonId();
        }
    }
}
