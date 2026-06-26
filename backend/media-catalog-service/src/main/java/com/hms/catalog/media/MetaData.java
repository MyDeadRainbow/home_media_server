package com.hms.catalog.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;

public record MetaData(String metaDataId, String plotSummary, LocalDate airDate, Float rating) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "metaDataId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return metaDataId;
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
                    + "plotSummary TEXT,"
                    + "airDate TEXT,"
                    + "rating REAL"
                    + ");";
        }

        @Override
        public PreparedStatementValue toInsertStatement(MetaData record) {
            return new PreparedStatementValue(
                    "INSERT INTO metadata (metaDataId, plotSummary, airDate, rating) VALUES (?, ?, ?, ?);",
                    new Object[] { record.metaDataId(), record.plotSummary(), record.airDate(), record.rating() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(MetaData record) {
            return new PreparedStatementValue(
                    "UPDATE metadata SET plotSummary = ?, airDate = ?, rating = ? WHERE metaDataId = ?;",
                    new Object[] { record.plotSummary(), record.airDate(), record.rating(), record.metaDataId() });
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
                    rs.getString("plotSummary"),
                    rs.getObject("airDate", LocalDate.class),
                    rs.getFloat("rating"));
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
