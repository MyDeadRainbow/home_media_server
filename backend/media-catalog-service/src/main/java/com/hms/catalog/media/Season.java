package com.hms.catalog.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.GetConnectionException;
import com.hms.shared.dao.PreparedStatementValue;
import com.hms.shared.dao.SQLiteRecord;
import com.hms.shared.dao.SQLiteRecordDao;

public record Season(String seasonId, String seriesId,
        String name, int seasonNumber, List<Episode> episodes)
        implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "seasonId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return seasonId;
    }

    public Season withName(String newName) {
        return new Season(this.seasonId, this.seriesId, newName, this.seasonNumber, this.episodes);
    }

    public Season withEpisodes(List<Episode> newEpisodes) {
        return new Season(this.seasonId, this.seriesId, this.name, this.seasonNumber, newEpisodes);
    }

    public Season withSeasonNumber(int newSeasonNumber) {
        return new Season(this.seasonId, this.seriesId, this.name, newSeasonNumber, this.episodes);
    }

    public Season withSeriesId(String newSeriesId) {
        return new Season(this.seasonId, newSeriesId, this.name, this.seasonNumber, this.episodes);
    }

    public Season withSeasonId(String newSeasonId) {
        return new Season(newSeasonId, this.seriesId, this.name, this.seasonNumber, this.episodes);
    }

    public Season addEpisode(Episode newEpisode) {
        Preconditions.checkArgument(newEpisode.seasonId().equals(this.seasonId),
                "Episode seasonId must match Season seasonId");
        List<Episode> updatedEpisodes = new ArrayList<>(List.copyOf(this.episodes));
        updatedEpisodes.removeIf(episode -> episode.episodeId().equals(newEpisode.episodeId()));
        updatedEpisodes.add(newEpisode);
        return new Season(this.seasonId, this.seriesId, this.name, this.seasonNumber, updatedEpisodes);
    }

    public Season removeEpisode(Episode episodeToRemove) {
        List<Episode> updatedEpisodes = new ArrayList<>(List.copyOf(this.episodes));
        updatedEpisodes.removeIf(episode -> episode.episodeId().equals(episodeToRemove.episodeId()));
        return new Season(this.seasonId, this.seriesId, this.name, this.seasonNumber, updatedEpisodes);
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
                    + "name TEXT NOT NULL,"
                    + "seasonNumber INTEGER NOT NULL,"
                    + "FOREIGN KEY(seriesId) REFERENCES series(seriesId)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Season record) {
            return new PreparedStatementValue(
                    "INSERT INTO seasons (seasonId, seriesId, name, seasonNumber) VALUES (?, ?, ?, ?);",
                    new Object[] { record.seasonId(), record.seriesId(), record.name(), record.seasonNumber() });
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
                    "UPDATE seasons SET seriesId = ?, name = ?, seasonNumber = ? WHERE seasonId = ?;",
                    new Object[] { record.seriesId(), record.name(), record.seasonNumber(), record.seasonId() });
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
            String name = rs.getString("name");
            int seasonNumber = rs.getInt("seasonNumber");
            List<Episode> episodes = new Episode.Dao().select(Map.of("seasonId", seasonId));
            return new Season(seasonId, seriesId, name, seasonNumber, episodes);
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
