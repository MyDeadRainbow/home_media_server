package com.hms.catalog.media;

import java.util.UUID;

import com.hms.shared.media.MediaItem;
import com.hms.shared.media.Movie;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;

public class MovieParser {
    private ParseEntry filePath;

    public MovieParser(ParseEntry filePath) {
        this.filePath = filePath;
    }

    public Movie parse() {
        String name = filePath.filePath();
        name = name.replaceAll("\\.?[0-9]{3,4}p.*$", "");

        MediaItem movieItem = new MediaItem(
                filePath.mediaId(),
                filePath.filePath() // Placeholder for video URL
        );
        // Handle movie creation logic here
        Movie movie = Movie.create(
                movieItem, // Placeholder for video URL
                MetaData.create(
                        name,
                        null,
                        null,
                        null,
                        MetaDataStatus.PENDING,
                        null),
                null // Placeholder for poster
        );

        return movie;
    }
}
