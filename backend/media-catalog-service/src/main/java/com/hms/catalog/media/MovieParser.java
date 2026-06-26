package com.hms.catalog.media;

import java.util.UUID;

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
        Movie movie = new Movie(
                UUID.randomUUID().toString(),
                name,
                movieItem, // Placeholder for video URL
                new MetaData(UUID.randomUUID().toString(), null, null, null));

        return movie;
    }
}
