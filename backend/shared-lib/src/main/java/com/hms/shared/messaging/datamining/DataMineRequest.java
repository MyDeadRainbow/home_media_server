package com.hms.shared.messaging.datamining;

import com.hms.shared.media.MediaCategory;

/**
 * DataMineRequest is a record that represents a request to mine data for a specific media item. It contains the following fields:
 * - mediaId: The unique identifier of the media item.
 * - title: The title of the media item.
 * - season: The season of the media item (if applicable).
 * - seriesTitle: The title of the series (if applicable).
 * - mediaType: The category of the media item.
 */
// public record DataMineRequest(String mediaId, String title, String season, String seriesTitle, MediaCategory mediaType)
//         implements com.hms.shared.messaging.JsonSerializable<DataMineRequest> {
//     public static final String TOPIC = "data-mine-requests";
// }

public interface DataMineRequest extends com.hms.shared.messaging.JsonSerializable<DataMineRequest> {
    public static final String TOPIC = "data-mine-requests";
    
    public String title();

    public static record Series(String seriesId, String seriesTitle) implements DataMineRequest {
        @Override
        public String title() {
            return seriesTitle;
        }
    }

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
