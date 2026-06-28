package com.hms.shared.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hms.shared.media.MediaCategory;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.shared.messaging.datamining.DataMineRequest;
import com.hms.shared.messaging.metadata.MetaData;
import com.hms.shared.messaging.mediaupdates.MediaUpdate;
import com.hms.shared.messaging.mediaupdates.MediaUpdateType;

public class JsonSerializableTest {

    @Test
    @DisplayName("Test serialization and deserialization of a CatalogUpdate object")
    public void testCatalogUpdateSerializable() {
        // Test serialization and deserialization of a CatalogUpdate object
        CatalogUpdate original = new CatalogUpdate("media123", CatalogUpdateType.CREATED, "Test Title",
                MediaCategory.MOVIE, 2023, "Test Description");
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        CatalogUpdate deserialized = assertDoesNotThrow(() -> JsonSerializable.fromJson(json, CatalogUpdate.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a MetaData.Episode object")
    public void testMetaDataEpisodeSerializable() {
        // Test serialization and deserialization of a MetaData.Episode object
        MetaData.Episode original = new MetaData.Episode("media123", "Test Title", "Test summary", LocalDate.now(),
                6.5f);
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        MetaData.Episode deserialized = assertDoesNotThrow(
                () -> JsonSerializable.fromJson(json, MetaData.Episode.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a MetaData.Episode object with null value")
    public void testMetaDataEpisodeSerializableWithNullValue() {
        // Test serialization and deserialization of a MetaData.Episode object with null
        // value
        MetaData.Episode original = new MetaData.Episode("media123", "Test Title", "Test summary", null, 6.5f);
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        MetaData.Episode deserialized = assertDoesNotThrow(
                () -> JsonSerializable.fromJson(json, MetaData.Episode.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a MetaData.Movie object")
    public void testMetaDataMovieSerializable() {
        // Test serialization and deserialization of a MetaData.Movie object
        MetaData.Movie original = new MetaData.Movie("movie123", "Movie Title", "Movie summary", LocalDate.now(), 8.2f);
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        MetaData.Movie deserialized = assertDoesNotThrow(() -> JsonSerializable.fromJson(json, MetaData.Movie.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

    @Test
    @DisplayName("Test serialization and deserialization of a MetaData.Series object")
    public void testMetaDataSeriesSerializable() {
        // Test serialization and deserialization of a MetaData.Series object
        MetaData.Series original = new MetaData.Series("series123", "Series Title", "Series summary", LocalDate.now(),
                7.8f, List.of(
                        new MetaData.Season("series123", 1, List.of(

                                new MetaData.Episode("episode123", "Episode Title", "Episode summary", LocalDate.now(),
                                        7.5f)))));
        String json = assertDoesNotThrow(() -> original.toJson().toString());

        MetaData.Series deserialized = assertDoesNotThrow(() -> JsonSerializable.fromJson(json, MetaData.Series.class));
        assertEquals(original, deserialized, "Deserialized object does not match the original");
    }

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

}
