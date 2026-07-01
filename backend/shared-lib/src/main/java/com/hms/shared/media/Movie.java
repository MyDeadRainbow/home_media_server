package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.messaging.JsonSerializable;

public record Movie(String movieId, MediaItem mediaItem, MetaData metaData)
        implements SQLiteRecord, JsonSerializable<Movie>, Title {

    @Override
    public String getPrimaryKeyField() {
        return "movieId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return movieId;
    }

    public static Movie create(MediaItem mediaItem, MetaData metaData) {
        String movieId = UUID.randomUUID().toString();
        return new Movie(movieId, mediaItem, metaData);
    }

    @Override
    public String title() {
        return metaData.title();
    }

    public Movie withMovieId(String newMovieId) {
        return new Movie(newMovieId, this.mediaItem, this.metaData);
    }

    public Movie withMediaItem(MediaItem newMediaItem) {
        return new Movie(this.movieId, newMediaItem, this.metaData);
    }

    public Movie withMetaData(MetaData newMetaData) {
        return new Movie(this.movieId, this.mediaItem, newMetaData);
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
                    "INSERT INTO movies (movieId, mediaId, metaDataId) VALUES (?, ?, ?);",
                    new Object[] { record.movieId(), record.mediaItem().mediaId(),
                            record.metaData().metaDataId() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Movie record) {
            return new PreparedStatementValue(
                    "UPDATE movies SET mediaId = ?, metaDataId = ? WHERE movieId = ?;",
                    new Object[] { record.mediaItem().mediaId(), record.metaData().metaDataId(),
                            record.movieId() });
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
