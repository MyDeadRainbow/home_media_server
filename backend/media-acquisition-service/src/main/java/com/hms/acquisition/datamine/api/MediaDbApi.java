package com.hms.acquisition.datamine.api;

import com.hms.shared.media.Movie;
import com.hms.shared.media.Series;

public interface MediaDbApi {

    public Series searchSeries(Series series);

    public Movie searchMovie(Movie movie);
}
