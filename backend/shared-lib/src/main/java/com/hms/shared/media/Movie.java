package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.poster.Poster;
import com.hms.shared.messaging.JsonSerializable;

public record Movie(String movieId, MediaItem mediaItem, MetaData metaData, Poster poster)
        implements SQLiteRecord, JsonSerializable, Title {

    @Override
    public String getPrimaryKeyField() {
        return "movieId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return movieId;
    }

    public static Movie create(MediaItem mediaItem, MetaData metaData, Poster poster) {
        String movieId = UUID.randomUUID().toString();
        return new Movie(movieId, mediaItem, metaData, poster);
    }

    @Override
    public String title() {
        return metaData.title();
    }

    public Movie withMovieId(String newMovieId) {
        return new Movie(newMovieId, this.mediaItem, this.metaData, this.poster);
    }

    public Movie withMediaItem(MediaItem newMediaItem) {
        return new Movie(this.movieId, newMediaItem, this.metaData, this.poster);
    }

    public Movie withMetaData(MetaData newMetaData) {
        return new Movie(this.movieId, this.mediaItem, newMetaData, this.poster);
    }

    public Movie withPoster(Poster newPoster) {
        return new Movie(this.movieId, this.mediaItem, this.metaData, newPoster);
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
                    + "posterId TEXT NOT NULL,"
                    + "FOREIGN KEY(mediaId) REFERENCES media_items(mediaId),"
                    + "FOREIGN KEY(metaDataId) REFERENCES metadata(metaDataId),"
                    + "FOREIGN KEY(posterId) REFERENCES posters(posterId)"
                    + ");";
        }

        @Override
        public void delete(Movie record) throws SQLException {
            new MediaItem.Dao().delete(record.mediaItem());
            new MetaData.Dao().delete(record.metaData());
            new Poster.Dao().delete(record.poster());
            super.delete(record);
        }

        @Override
        public void insert(Movie record) throws SQLException {
            new MediaItem.Dao().insert(record.mediaItem());
            new MetaData.Dao().insert(record.metaData());
            new Poster.Dao().insert(record.poster());
            super.insert(record);
        }

        @Override
        public void update(Movie record) throws SQLException {
            new MediaItem.Dao().update(record.mediaItem());
            new MetaData.Dao().update(record.metaData());
            new Poster.Dao().update(record.poster());
            super.update(record);
        }

        @Override
        public PreparedStatementValue toInsertStatement(Movie record) {
            return new PreparedStatementValue(
                    "INSERT INTO movies (movieId, mediaId, metaDataId, posterId) VALUES (?, ?, ?, ?);",
                    new Object[] { record.movieId(), record.mediaItem().mediaId(),
                            record.metaData().metaDataId(), record.poster().posterId() });
        }

        @Override
        public PreparedStatementValue toUpdateStatement(Movie record) {
            return new PreparedStatementValue(
                    "UPDATE movies SET mediaId = ?, metaDataId = ?, posterId = ? WHERE movieId = ?;",
                    new Object[] { record.mediaItem().mediaId(), record.metaData().metaDataId(),
                            record.poster().posterId(), record.movieId() });
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
                    new MetaData.Dao().get(rs.getString("metaDataId")),
                    new Poster.Dao().get(rs.getString("posterId")));
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
