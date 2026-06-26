package com.hms.shared.messaging.datamining;

/**
 * DataMineRequest is a record that represents a request to mine data for a specific media item.
 */
public interface DataMineRequest extends com.hms.shared.messaging.JsonSerializable<DataMineRequest> {
    public static final String TOPIC = "data-mine-requests";
    
    public String title();

    public static record Series(String seriesId, String seriesTitle) implements DataMineRequest {
        @Override
        public String title() {
            return seriesTitle;
        }
    }

    /**
     * (String mediaId, String episodeId, String episodeTitle, int episodeNumber, String seriesTitle, int seasonNumber)
     */
    public static record Episode(String mediaId, String episodeId, String episodeTitle, int episodeNumber, String seriesTitle, int seasonNumber) implements DataMineRequest {
        @Override
        public String title() {
            return episodeTitle;
        }
    }

    public static record Movie(String mediaId, String movieId, String movieTitle) implements DataMineRequest {
        @Override
        public String title() {
            return movieTitle;
        }
    }

}
