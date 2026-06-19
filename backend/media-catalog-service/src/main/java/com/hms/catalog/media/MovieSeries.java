package com.hms.catalog.media;

import java.util.List;

public record MovieSeries(String name, List<Movie> movies) {
}
