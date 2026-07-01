package com.hms.shared.messaging;

public class Topics {
    public static final String CATALOG_UPDATES = "catalog-updates";
    
    public static final String MOVIE = "movie";
    public static final String SERIES = "series";
    
    public static final String METADATA = "metadata";
    public static final String METADATA_MOVIE = METADATA + "-" + MOVIE;
    public static final String METADATA_SERIES = METADATA + "-" + SERIES;
    
    public static final String DATAMINE = "datamine";
    public static final String DATAMINE_MOVIE = DATAMINE + "-" + MOVIE;
    public static final String DATAMINE_SERIES = DATAMINE + "-" + SERIES;
}
