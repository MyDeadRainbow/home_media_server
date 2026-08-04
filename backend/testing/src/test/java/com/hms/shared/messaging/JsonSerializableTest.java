package com.hms.shared.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hms.shared.media.Episode;
import com.hms.shared.media.MediaItem;
import com.hms.shared.media.MediaCategory;
import com.hms.shared.media.Movie;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;
import com.hms.shared.media.poster.Poster;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.shared.messaging.datamining.DataMineRequest;
// import com.hms.shared.messaging.metadata.MetaData;
import com.hms.shared.messaging.mediaupdates.MediaUpdate;
import com.hms.shared.messaging.mediaupdates.MediaUpdateType;

public class JsonSerializableTest {

    private <T extends JsonSerializable> void assertJsonRoundTrip(T original, Class<T> clazz) {
        String json = assertDoesNotThrow(() -> ((JsonSerializable) original).toJson().toString());
        T deserialized = assertDoesNotThrow(() -> JsonSerializable.fromJson(json, clazz));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    private MetaData createMetaData(String idSuffix, String title) {
        return new MetaData(
                "meta-" + idSuffix,
                title,
                "Plot for " + title,
                LocalDate.of(2024, 1, 10),
                8.4f,
                MetaDataStatus.COMPLETE,
                "metadata ready");
    }

    private MediaItem createMediaItem(String idSuffix) {
        return new MediaItem("media-" + idSuffix, "/library/" + idSuffix + ".mp4");
    }

    private Poster createPoster(String idSuffix) {
        return new Poster("poster-" + idSuffix, "https://example.com/poster-" + idSuffix + ".jpg");
    }

    private Episode createEpisode() {
        return new Episode(
                "episode-1",
                "season-1",
                "series-1",
                1,
                createMediaItem("episode-1"),
                createMetaData("episode-1", "Pilot"),
                createPoster("episode-1"));
    }

    private Season createSeason() {
        return new Season(
                "season-1",
                "series-1",
                1,
                createMetaData("season-1", "Season 1"),
                createPoster("season-1"),
                List.of(createEpisode()));
    }

    private Series createSeries() {
        return new Series(
                "series-1",
                createMetaData("series-1", "Example Series"),
                createPoster("series-1"),
                List.of(createSeason()));
    }

    @Test
    @DisplayName("Test serialization and deserialization of a CatalogUpdate object")
    public void testCatalogUpdateSerializable() {
        // Test serialization and deserialization of a CatalogUpdate object
        CatalogUpdate original = new CatalogUpdate(CatalogUpdateType.CREATED, MediaCategory.MOVIE,
                List.of(new com.hms.shared.messaging.catalogupdates.FilePathRecord("media123", "/path/to/file")));
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        CatalogUpdate deserialized = assertDoesNotThrow(() -> JsonSerializable.fromJson(json, CatalogUpdate.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    // @Test
    // @DisplayName("Test serialization and deserialization of a MetaData.Episode
    // object")
    // public void testMetaDataEpisodeSerializable() {
    // // Test serialization and deserialization of a MetaData.Episode object
    // MetaData.Episode original = new MetaData.Episode("media123", "Test Title",
    // "Test summary", LocalDate.now(),
    // 6.5f, new MetaData.Base(MetaData.Status.SUCCESS, "Episode metadata retrieved
    // successfully"));
    // String json = assertDoesNotThrow(() -> original.toJson().toString());

    // MetaData.Episode deserialized = assertDoesNotThrow(
    // () -> JsonSerializable.fromJson(json, MetaData.Episode.class));
    // assertEquals(original, deserialized, "Deserialized object does not match the
    // original");
    // }

    // @Test
    // @DisplayName("Test serialization and deserialization of a MetaData.Episode
    // object with null value")
    // public void testMetaDataEpisodeSerializableWithNullValue() {
    // // Test serialization and deserialization of a MetaData.Episode object with
    // null
    // // value
    // MetaData.Episode original = new MetaData.Episode("media123", "Test Title",
    // "Test summary", null, 6.5f,
    // new MetaData.Base(MetaData.Status.SUCCESS, "Episode metadata retrieved
    // successfully"));
    // String json = assertDoesNotThrow(() -> original.toJson().toString());

    // MetaData.Episode deserialized = assertDoesNotThrow(
    // () -> JsonSerializable.fromJson(json, MetaData.Episode.class));
    // assertEquals(original, deserialized, "Deserialized object does not match the
    // original");
    // }

    // @Test
    // @DisplayName("Test serialization and deserialization of a MetaData.Movie
    // object")
    // public void testMetaDataMovieSerializable() {
    // // Test serialization and deserialization of a MetaData.Movie object
    // MetaData.Movie original = new MetaData.Movie("movie123", "Movie Title",
    // "Movie summary", LocalDate.now(), 8.2f,
    // new MetaData.Base(MetaData.Status.SUCCESS, "Movie metadata retrieved
    // successfully"));
    // String json = assertDoesNotThrow(() -> original.toJson().toString());

    // MetaData.Movie deserialized = assertDoesNotThrow(() ->
    // JsonSerializable.fromJson(json, MetaData.Movie.class));
    // assertEquals(original, deserialized, "Deserialized object does not match the
    // original");
    // }

    // @Test
    // @DisplayName("Test serialization and deserialization of a MetaData.Series
    // object")
    // public void testMetaDataSeriesSerializable() {
    // // Test serialization and deserialization of a MetaData.Series object
    // MetaData.Series original = new MetaData.Series("series123", "Series Title",
    // "Series summary", LocalDate.now(),
    // 7.8f, List.of(
    // new MetaData.Season("series123", 1, List.of(
    // new MetaData.Episode("episode123", "Episode Title", "Episode summary",
    // LocalDate.now(),
    // 7.5f,
    // new MetaData.Base(MetaData.Status.SUCCESS,
    // "Episode metadata retrieved successfully"))),
    // new MetaData.Base(MetaData.Status.SUCCESS, "Season metadata retrieved
    // successfully"))),
    // new MetaData.Base(MetaData.Status.SUCCESS, "Series metadata retrieved
    // successfully"));
    // String json = assertDoesNotThrow(() -> original.toJson().toString());

    // MetaData.Series deserialized = assertDoesNotThrow(() ->
    // JsonSerializable.fromJson(json, MetaData.Series.class));
    // assertEquals(original, deserialized, "Deserialized object does not match the
    // original");
    // }

    @Test
    @DisplayName("Test serialization and deserialization of a MediaUpdate object")
    public void testMediaUpdateSerializable() {
        // Test serialization and deserialization of a MediaUpdate object
        MediaUpdate original = new MediaUpdate(MediaUpdateType.UPDATED, "media123");
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        MediaUpdate deserialized = assertDoesNotThrow(() -> JsonSerializable.fromJson(json, MediaUpdate.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a DataMineRequest.Movie object")
    public void testDataMineRequestMovieSerializable() {
        // Test serialization and deserialization of a DataMineRequest.Movie object
        DataMineRequest.Movie original = new DataMineRequest.Movie("media123", "movie123", "Movie Title");
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        DataMineRequest.Movie deserialized = assertDoesNotThrow(
                () -> JsonSerializable.fromJson(json, DataMineRequest.Movie.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a DataMineRequest.Series object")
    public void testDataMineRequestSeriesSerializable() {
        // Test serialization and deserialization of a DataMineRequest.Series object
        DataMineRequest.Series original = new DataMineRequest.Series("series123", "Series Title", List.of(
                new DataMineRequest.Season("series123", 1, List.of(
                        new DataMineRequest.Episode("media123", "episode123", "Episode Title", 1, "Series Title",
                                1)))));
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        DataMineRequest.Series deserialized = assertDoesNotThrow(
                () -> JsonSerializable.fromJson(json, DataMineRequest.Series.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a DataMineRequest.Episode object")
    public void testDataMineRequestEpisodeSerializable() {
        // Test serialization and deserialization of a DataMineRequest.Episode object
        DataMineRequest.Episode original = new DataMineRequest.Episode("media123", "episode123", "Episode Title", 4,
                "Series Title", 2);
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        DataMineRequest.Episode deserialized = assertDoesNotThrow(
                () -> JsonSerializable.fromJson(json, DataMineRequest.Episode.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a MediaItem object")
    public void testMediaItemSerializable() {
        assertJsonRoundTrip(createMediaItem("movie-1"), MediaItem.class);
    }

    @Test
    @DisplayName("Test serialization and deserialization of a MetaData object")
    public void testMetaDataSerializable() {
        assertJsonRoundTrip(createMetaData("movie-1", "Example Movie"), MetaData.class);
    }

    @Test
    @DisplayName("Test serialization and deserialization of a Movie object")
    public void testMovieSerializable() {
        Movie original = new Movie(
                "movie-1",
                createMediaItem("movie-1"),
                createMetaData("movie-1", "Example Movie"),
                createPoster("movie-1"));
        assertJsonRoundTrip(original, Movie.class);
    }

    @Test
    @DisplayName("Test serialization and deserialization of an Episode object")
    public void testEpisodeSerializable() {
        assertJsonRoundTrip(createEpisode(), Episode.class);
    }

    @Test
    @DisplayName("Test serialization and deserialization of a Season object")
    public void testSeasonSerializable() {
        assertJsonRoundTrip(createSeason(), Season.class);
    }

    @Test
    @DisplayName("Test serialization and deserialization of a Series object")
    public void testSeriesSerializable() {
        assertJsonRoundTrip(createSeries(), Series.class);
    }

}
