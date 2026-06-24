package com.hms.catalog.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.hms.dao.DBFileNotFoundException;
import com.hms.dao.GetConnectionException;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;

public record Series(String seriesId, String name,
        List<Season> seasons) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "seriesId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return seriesId;
    }

    public Series withName(String newName) {
        return new Series(this.seriesId, newName, this.seasons);
    }

    public Series withSeasons(List<Season> newSeasons) {
        return new Series(this.seriesId, this.name, newSeasons);
    }

    public Series addSeason(Season newSeason) {
        Preconditions.checkArgument(newSeason.seriesId().equals(this.seriesId),
                "Season seriesId must match Series seriesId");
        List<Season> updatedSeasons = new ArrayList<>(List.copyOf(this.seasons));
        updatedSeasons.removeIf(season -> season.seasonId().equals(newSeason.seasonId()));
        updatedSeasons.add(newSeason);
        return new Series(this.seriesId, this.name, updatedSeasons);
    }

    public Series removeSeason(Season seasonToRemove) {
        List<Season> updatedSeasons = new ArrayList<>(List.copyOf(this.seasons));
        updatedSeasons.removeIf(season -> season.seasonId().equals(seasonToRemove.seasonId()));
        return new Series(this.seriesId, this.name, updatedSeasons);
    }

    public static class Dao extends SQLiteRecordDao<Series> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "series";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS series ("
                    + "seriesId TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Series record) {
            return new PreparedStatementValue(
                    "INSERT INTO series (seriesId, name) VALUES (?, ?);",
                    new Object[] { record.seriesId(), record.name() });
        }

        @Override
        public void insert(Series record) throws SQLException {
            super.insert(record);
            for (Season season : record.seasons()) {
                new Season.Dao().insert(season);
            }
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Series record) {
            return new PreparedStatementValue(
                    "UPDATE series SET name = ? WHERE seriesId = ?;",
                    new Object[] { record.name(), record.seriesId() });
        }

        @Override
        public void update(Series record) throws SQLException {
            super.update(record);
            for (Season season : record.seasons()) {
                new Season.Dao().update(season);
            }
        }

        @Override
        public PreparedStatementValue toDeleteStatement(Series record) {
            return new PreparedStatementValue(
                    "DELETE FROM series WHERE seriesId = ?;",
                    new Object[] { record.seriesId() });
        }

        @Override
        public void delete(Series record) throws SQLException {
            for (Season season : record.seasons()) {
                new Season.Dao().delete(season);
            }
            super.delete(record);
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            Preconditions.checkNotNull(conditions, "Conditions map cannot be null");
            StringBuilder query = new StringBuilder("SELECT * FROM series");
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
        public Series mapResultSetToRecord(ResultSet rs) throws SQLException {
            String seriesId = rs.getString("seriesId");
            String name = rs.getString("name");
            List<Season> seasons = new Season.Dao().select(Map.of("seriesId", seriesId));
            return new Series(seriesId, name, seasons);
        }

        @Override
        public String getPrimaryKeyField() {
            return "seriesId";
        }

        @Override
        public Object getPrimaryKeyValue(Series record) {
            return record.seriesId();
        }

    }
}
