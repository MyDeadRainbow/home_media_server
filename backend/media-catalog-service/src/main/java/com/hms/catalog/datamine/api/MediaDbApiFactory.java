package com.hms.catalog.datamine.api;

import java.util.Optional;

public class MediaDbApiFactory {
    public static MediaDbApi createTMDBApi() {
        return new TMDBApi(Optional.of(System.getenv("TMDB_API_KEY")).get());
    }

    // public static MediaDbApi createOMDBApi() {

    // }

    // public static MediaDbApi createTvMazeApi() {

    // }
}
