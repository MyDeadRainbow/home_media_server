package com.hms.shared.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hms.dao.DBFileNotFoundException;
import com.hms.dao.Database;
import com.hms.dao.GetConnectionException;
import com.hms.dao.PreparedStatementValue;
import com.hms.dao.SQLiteRecord;
import com.hms.dao.SQLiteRecordDao;

public record MediaInfo(String mediaId, String title, String type, LocalDate releaseDate, String plotSummary,
        Float rating, String streamUrl) implements SQLiteRecord {

    @Override
    public String getPrimaryKeyField() {
        return "mediaId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return mediaId;
    }

    public static class Dao extends com.hms.dao.SQLiteRecordDao<MediaInfo> {

        @Override
        public List<SQLiteRecordDao<?>> getDependecies() {
            return List.of(new Episode.Dao(), new Movie.Dao(), new Season.Dao(), new Series.Dao());
        }

        @Override
        public String getDbPath() {
            return "media_catalog.db";
        }

        @Override
        public String getTableName() {
            return "media_info";
        }

        @Override
        public void ensureTableExists() throws SQLException {
            try (var conn = Database.getConnection(getDbPath());) {
                for (SQLiteRecordDao<?> dao : getDependecies()) {
                    dao.ensureTableExists();
                }
                try (var stmt = conn.createStatement()) {
                    stmt.execute("DROP VIEW IF EXISTS " + getTableName() + "; ");

                    stmt.execute(toCreateTableStatement());

                }
            } catch (DBFileNotFoundException | GetConnectionException e) {
                throw new SQLException("Failed to get database connection", e);
            }
        }

        @Override
        public String toCreateTableStatement() {
            return "CREATE VIEW " + getTableName() + " AS "
                    + "SELECT media_items.mediaId, series_metadata.title AS seriesName, seasons.seasonNumber, "
                    + "episodes_metadata.title AS episodeName, episodes.episodeNumber, movies_metadata.title AS movieName, media_items.filePath, "
                    + "base_metadata.plotSummary, base_metadata.rating, base_metadata.airDate, "
                    + "concat_ws('', episodes_metadata.title, movies_metadata.title) AS name, "
                    + "concat_ws(' ', series_metadata.title, episodes_metadata.title, movies_metadata.title) AS search "
                    + "FROM media_items "
                    + "LEFT JOIN episodes ON media_items.mediaId = episodes.mediaId "
                    + "LEFT JOIN seasons ON seasons.seasonId = episodes.seasonId "
                    + "LEFT JOIN series ON series.seriesId = episodes.seriesId "
                    + "LEFT JOIN movies ON movies.mediaId = media_items.mediaId "
                    + "LEFT JOIN metadata base_metadata ON base_metadata.metaDataId = series.metaDataId OR base_metadata.metaDataId = seasons.metaDataId OR base_metadata.metaDataId = episodes.metaDataId OR base_metadata.metaDataId = movies.metaDataId "
                    + "LEFT JOIN metadata series_metadata ON series_metadata.metaDataId = series.metaDataId "
                    + "LEFT JOIN metadata episodes_metadata ON episodes_metadata.metaDataId = episodes.metaDataId "
                    + "LEFT JOIN metadata movies_metadata ON movies_metadata.metaDataId = movies.metaDataId"
                    + "GROUP BY media_items.mediaId";
        }

        @Override
        public PreparedStatementValue toInsertStatement(MediaInfo record) {
            throw new UnsupportedOperationException("Unimplemented method 'toInsertStatement'");
        }

        @Override
        public PreparedStatementValue toUpdateStatement(MediaInfo record) {
            throw new UnsupportedOperationException("Unimplemented method 'toUpdateStatement'");
        }

        @Override
        public PreparedStatementValue toDeleteStatement(MediaInfo record) {
            throw new UnsupportedOperationException("Unimplemented method 'toDeleteStatement'");
        }

        @Override
        public PreparedStatementValue toSelectStatement(Map<String, Object> conditions) {
            return new PreparedStatementValue(
                    "SELECT * FROM " + getTableName() + " WHERE search LIKE ?;",
                    new Object[] { "%" + conditions.get("search") + "%" });
        }

        public List<MediaInfo> search(String query) throws SQLException {
            Map<String, Object> conditions = Map.of("search", Optional.ofNullable(query).orElse(""));
            return select(conditions);
        }

        @Override
        public MediaInfo mapResultSetToRecord(ResultSet rs) throws SQLException {
            return new MediaInfo(
                    rs.getString("mediaId"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getObject("airDate") != null
                            ? rs.getObject("airDate", LocalDate.class)
                            : null,
                    rs.getString("plotSummary"),
                    rs.getObject("rating") != null
                            ? rs.getFloat("rating")
                            : null,
                    rs.getString("filePath"));
        }

        @Override
        public String getPrimaryKeyField() {
            return "mediaId";
        }

        @Override
        public Object getPrimaryKeyValue(MediaInfo record) {
            return record.mediaId();
        }
    }

}
