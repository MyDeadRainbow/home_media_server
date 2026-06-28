package com.hms.shared.messaging.datamining;

import java.util.List;

import com.hms.shared.messaging.JsonSerializable;

/**
 * DataMineRequest is a record that represents a request to mine data for a specific media item.
 */
public interface DataMineRequest extends JsonSerializable<DataMineRequest> {
    public static final String TOPIC = "data-mine-requests";
    
    public String imdbSearchTitle();

    public static record Series(String seriesId, String seriesTitle, List<Season> seasons) implements DataMineRequest {
        @Override
        public String imdbSearchTitle() {
            return seriesTitle;
        }
    }

    public static record Season(String seriesId, int seasonNumber, List<Episode> episodes) implements JsonSerializable<Season> {
        // @Override
        // public String imdbSearchTitle() {
        //     return seasonTitle;
        // }
    }

    /**
     * (String mediaId, String episodeId, String episodeTitle, int episodeNumber, String seriesTitle, int seasonNumber)
     */
    public static record Episode(String mediaId, String episodeId, String episodeTitle, int episodeNumber, String seriesTitle, int seasonNumber) implements JsonSerializable<Episode> {
        // /**
        //  * returns the series title so that the search can be done on imdb for the series, then the season and episode can be found from there
        //  * @return seriesTitle
        //  */
        // @Override
        // public String imdbSearchTitle() {
        //     return seriesTitle;
        // }
    }

    public static record Movie(String mediaId, String movieId, String movieTitle) implements DataMineRequest {
        @Override
        public String imdbSearchTitle() {
            return movieTitle;
        }
    }

}
