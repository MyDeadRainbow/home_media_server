package com.hms.shared.messaging.metadata;

import java.time.LocalDate;
import java.util.List;

import com.hms.shared.messaging.JsonSerializable;

public interface MetaData extends JsonSerializable<MetaData> {
    public static final String TOPIC = "media-metadata";

    /**
     * (String episodeId, String title, String plotSummary, LocalDate airDate, Float rating)
     */
    public static record Episode(String episodeId, String title, String plotSummary, LocalDate airDate, Float rating)
            implements MetaData {
    }

    /**
     * (String movieId, String title, String plotSummary, LocalDate releaseDate, Float rating)
     */
    public static record Movie(String movieId, String title, String plotSummary, LocalDate releaseDate, Float rating)
            implements MetaData {
    }

    /**
     * (String seriesId, String title, String plotSummary, LocalDate firstAirDate, Float rating)
     */
    public static record Series(String seriesId, String title, String plotSummary, LocalDate firstAirDate, Float rating, List<Season> seasons)
            implements MetaData {
    }

    public static record Season(String seriesId, int seasonNumber, List<Episode> episodes) implements MetaData {
    }
}
