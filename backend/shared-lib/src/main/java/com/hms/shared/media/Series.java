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

public record Series(String seriesId, MetaData metaData,
        List<Season> seasons) implements SQLiteRecord, JsonSerializable<Series>, Title {

    @Override
    public String getPrimaryKeyField() {
        return "seriesId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return seriesId;
    }

    
    public static Series create(MetaData metaData, List<Season> seasons) {
        String seriesId = UUID.randomUUID().toString();
        return new Series(seriesId, metaData, seasons);
    }

    @Override
    public String title() {
        return metaData.title();
    }

    public Series withSeriesId(String newSeriesId) {
        return new Series(newSeriesId, this.metaData, this.seasons);
    }

    public Series withTitle(String newTitle) {
        return new Series(this.seriesId, this.metaData.withTitle(newTitle), this.seasons);
    }

    public Series withSeasons(List<Season> newSeasons) {
        return new Series(this.seriesId, this.metaData, newSeasons);
    }

    public Series withMetaData(MetaData newMetaData) {
        return new Series(this.seriesId, newMetaData, this.seasons);
    }

    public Series addSeason(Season newSeason) {
        Preconditions.checkArgument(newSeason.seriesId().equals(this.seriesId),
                "Season seriesId must match Series seriesId");
        List<Season> updatedSeasons = new ArrayList<>(List.copyOf(this.seasons));
        updatedSeasons.removeIf(season -> season.seasonId().equals(newSeason.seasonId()));
        updatedSeasons.add(newSeason);
        return new Series(this.seriesId, this.metaData, updatedSeasons);
    }

    public Series removeSeason(Season seasonToRemove) {
        List<Season> updatedSeasons = new ArrayList<>(List.copyOf(this.seasons));
        updatedSeasons.removeIf(season -> season.seasonId().equals(seasonToRemove.seasonId()));
        return new Series(this.seriesId, this.metaData, updatedSeasons);
    }

    public Series replaceSeason(Season newSeason) {
        Preconditions.checkArgument(newSeason.seriesId().equals(this.seriesId),
                "Season seriesId must match Series seriesId");
        List<Season> updatedSeasons = new ArrayList<>(List.copyOf(this.seasons));
        updatedSeasons.removeIf(season -> season.seasonId().equals(newSeason.seasonId()));
        updatedSeasons.add(newSeason);
        return new Series(this.seriesId, this.metaData, updatedSeasons);
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
                    + "metaDataId TEXT NOT NULL,"
                    + "FOREIGN KEY(metaDataId) REFERENCES metadata(metaDataId)"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(Series record) {
            return new PreparedStatementValue(
                    "INSERT INTO series (seriesId, metaDataId) VALUES (?, ?);",
                    new Object[] { record.seriesId(), record.metaData().metaDataId() });
        }

        @Override
        public void insert(Series record) throws SQLException {
            new MetaData.Dao().insert(record.metaData());
            super.insert(record);
            for (Season season : record.seasons()) {
                new Season.Dao().insert(season);
            }
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Series record) {
            return new PreparedStatementValue(
                    "UPDATE series SET metaDataId = ? WHERE seriesId = ?;",
                    new Object[] { record.metaData().metaDataId(), record.seriesId() });
        }

        @Override
        public void update(Series record) throws SQLException {
            new MetaData.Dao().update(record.metaData());
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
            new MetaData.Dao().delete(record.metaData());
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
            String metaDataId = rs.getString("metaDataId");
            MetaData metaData = new MetaData.Dao().get(metaDataId);
            List<Season> seasons = new Season.Dao().select(Map.of("seriesId", seriesId));
            return new Series(seriesId, metaData, seasons);
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
