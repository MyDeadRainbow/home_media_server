package com.hms.shared.media.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;
import com.hms.shared.media.Title;
import com.hms.shared.messaging.JsonSerializable;

public record MetaData(String metaDataId, String title, String plotSummary, LocalDate airDate, Float rating,
        MetaDataStatus status, String message)
        implements SQLiteRecord, JsonSerializable<MetaData>, Title {

    @Override
    public String getPrimaryKeyField() {
        return "metaDataId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return metaDataId;
    }

    public static MetaData create(String title, String plotSummary, LocalDate airDate, Float rating,
            MetaDataStatus status, String message) {
        String metaDataId = UUID.randomUUID().toString();
        return new MetaData(metaDataId, title, plotSummary, airDate, rating, status, message);
    }

    public MetaData withMetaDataId(String newMetaDataId) {
        return new MetaData(newMetaDataId, this.title, this.plotSummary, this.airDate, this.rating, this.status,
                this.message);
    }

    public MetaData withTitle(String newTitle) {
        return new MetaData(this.metaDataId, newTitle, this.plotSummary, this.airDate, this.rating, this.status,
                this.message);
    }

    public MetaData withPlotSummary(String newPlotSummary) {
        return new MetaData(this.metaDataId, this.title, newPlotSummary, this.airDate, this.rating, this.status,
                this.message);
    }

    public MetaData withAirDate(LocalDate newAirDate) {
        return new MetaData(this.metaDataId, this.title, this.plotSummary, newAirDate, this.rating, this.status,
                this.message);
    }

    public MetaData withRating(Float newRating) {
        return new MetaData(this.metaDataId, this.title, this.plotSummary, this.airDate, newRating, this.status,
                this.message);
    }

    public MetaData withStatus(MetaDataStatus newStatus) {
        return new MetaData(this.metaDataId, this.title, this.plotSummary, this.airDate, this.rating, newStatus,
                this.message);
    }

    public MetaData withMessage(String newMessage) {
        return new MetaData(this.metaDataId, this.title, this.plotSummary, this.airDate, this.rating, this.status,
                newMessage);
    }

    public static class Dao extends SQLiteRecordDao<MetaData> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "metadata";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS metadata ("
                    + "metaDataId TEXT PRIMARY KEY,"
                    + "title TEXT,"
                    + "plotSummary TEXT,"
                    + "airDate TEXT,"
                    + "rating REAL,"
                    + "status TEXT,"
                    + "message TEXT"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(MetaData record) {
            return new PreparedStatementValue(
                    "INSERT INTO metadata (metaDataId, title, plotSummary, airDate, rating, status, message) VALUES (?, ?, ?, ?, ?, ?, ?);",
                    new Object[] { record.metaDataId(), record.title(), record.plotSummary(), record.airDate(),
                            record.rating(), record.status(), record.message() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(MetaData record) {
            return new PreparedStatementValue(
                    "UPDATE metadata SET title = ?, plotSummary = ?, airDate = ?, rating = ?, status = ?, message = ? WHERE metaDataId = ?;",
                    new Object[] { record.title(), record.plotSummary(), record.airDate(), record.rating(),
                            record.status(), record.message(), record.metaDataId() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(MetaData record) {
            return new PreparedStatementValue(
                    "DELETE FROM metadata WHERE metaDataId = ?;",
                    new Object[] { record.metaDataId() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM metadata");
            if (conditions != null && !conditions.isEmpty()) {
                queryBuilder.append(" WHERE ");
                boolean firstCondition = true;
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    if (!firstCondition) {
                        queryBuilder.append(" AND ");
                    }
                    queryBuilder.append(entry.getKey()).append(" = ?");
                    firstCondition = false;
                }
            }
            queryBuilder.append(";");
            return new PreparedStatementValue(queryBuilder.toString(), conditions.values().toArray());
        }

        @Override
        public MetaData mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new MetaData(
                    rs.getString("metaDataId"),
                    rs.getString("title"),
                    rs.getString("plotSummary"),
                    rs.getObject("airDate", LocalDate.class),
                    rs.getFloat("rating"),
                    MetaDataStatus.valueOf(rs.getString("status")),
                    rs.getString("message"));
        }

        @Override
        public String getPrimaryKeyField() {
            return "metaDataId";
        }

        @Override
        public Object getPrimaryKeyValue(MetaData record) {
            return record.metaDataId();
        }
    }
}
