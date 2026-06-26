package com.hms.catalog.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;

public record Movie(String movieId, String name, MediaItem mediaItem, MetaData metaData) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "movieId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return movieId;
    }

    public Movie withName(String newName) {
        return new Movie(this.movieId, newName, this.mediaItem, this.metaData);
    }

    public Movie withMediaItem(MediaItem newMediaItem) {
        return new Movie(this.movieId, this.name, newMediaItem, this.metaData);
    }

    public Movie withMetaData(MetaData newMetaData) {
        return new Movie(this.movieId, this.name, this.mediaItem, newMetaData);
    }

    public static class Dao extends com.hms.dao.SQLiteRecordDao<Movie> {

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "movies";
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE TABLE IF NOT EXISTS movies ("
                    + "movieId TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "mediaId TEXT NOT NULL,"
                    + "metaDataId TEXT NOT NULL,"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(mediaId),"
                    + "FOREIGN KEY(metaDataId) REFERENCES metadata(metaDataId)"
                    + ");";
        }

        @Override
        public void delete(Movie record) throws SQLException {
            new MediaItem.Dao().delete(record.mediaItem());
            new MetaData.Dao().delete(record.metaData());
            super.delete(record);
        }

        @Override
        public void insert(Movie record) throws SQLException {
            new MediaItem.Dao().insert(record.mediaItem());
            new MetaData.Dao().insert(record.metaData());
            super.insert(record);
        }

        @Override
        public void update(Movie record) throws SQLException {
            new MediaItem.Dao().update(record.mediaItem());
            new MetaData.Dao().update(record.metaData());
            super.update(record);
        }

        @Override
        public PreparedStatementValue toInsertStatement(Movie record) {
            return new PreparedStatementValue(
                    "INSERT INTO movies (movieId, name, mediaId, metaDataId) VALUES (?, ?, ?, ?);",
                    new Object[] { record.movieId(), record.name(), record.mediaItem().mediaId(), record.metaData().metaDataId() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Movie record) {
            return new PreparedStatementValue(
                    "UPDATE movies SET name = ?, mediaId = ?, metaDataId = ? WHERE movieId = ?;",
                    new Object[] { record.name(), record.mediaItem().mediaId(), record.metaData().metaDataId(), record.movieId() });
        }

        @Override
        public PreparedStatementValue toDeleteStatement(Movie record) {
            return new PreparedStatementValue(
                    "DELETE FROM movies WHERE movieId = ?;",
                    new Object[] { record.movieId() });
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            StringBuilder sql = new StringBuilder("SELECT * FROM movies");
            if (conditions != null && !conditions.isEmpty()) {
                sql.append(" WHERE ");
                boolean first = true;
                for (String field : conditions.keySet()) {
                    if (!first) {
                        sql.append(" AND ");
                    }
                    sql.append(field).append(" = ?");
                    first = false;
                }
            }
            sql.append(";");
            return new PreparedStatementValue(sql.toString(), conditions.values().toArray());
        }

        @Override
        public Movie mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new Movie(
                    rs.getString("movieId"),
                    rs.getString("name"),
                    new MediaItem.Dao().get(rs.getString("mediaId")),
                    new MetaData.Dao().get(rs.getString("metaDataId")));
        }

        @Override
        public String getPrimaryKeyField() {
            return "movieId";
        }

        @Override
        public Object getPrimaryKeyValue(Movie record) {
            return record.movieId();
        }
    }
}
