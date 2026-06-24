package com.hms.catalog.media;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hms.shared.dao.DBFileNotFoundException;
import com.hms.shared.dao.Database;
import com.hms.shared.dao.GetConnectionException;
import com.hms.shared.dao.PreparedStatementValue;
import com.hms.shared.dao.SQLiteRecord;

public record MediaInfo(String mediaId, String title, String type, int year, String description, String posterUrl,
        String streamUrl) implements SQLiteRecord {
    @Override
    public String getPrimaryKeyField() {
        return "mediaId";
    }

    @Override
    public Object getPrimaryKeyValue() {
        return mediaId;
    }

    public static class Dao extends com.hms.shared.dao.SQLiteRecordDao<MediaInfo> {

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
                    + "SELECT media_items.mediaId, series.name AS seriesName, seasons.name AS seasonName, seasons.seasonNumber, "
                    + "episodes.name AS episodeName, episodes.episodeNumber, movies.name AS movieName, media_items.filePath, "
                    + "concat_ws('', episodes.name, movies.name) AS name, "
                    + "concat_ws(' ', series.name, seasons.name, episodes.name, movies.name) AS search "
                    + "FROM media_items "
                    + "LEFT JOIN episodes ON media_items.mediaId = episodes.mediaId "
                    + "LEFT JOIN seasons ON seasons.seasonId = episodes.seasonId "
                    + "LEFT JOIN series ON series.seriesId = episodes.seriesId "
                    + "LEFT JOIN movies ON movies.mediaId = media_items.mediaId;";
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
                    "episode",
                    0,
                    null,
                    null,
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
