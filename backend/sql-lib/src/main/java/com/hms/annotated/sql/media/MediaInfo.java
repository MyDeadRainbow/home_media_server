package com.hms.annotated.sql.media;

import com.hms.annotated.sql.ViewRecord;
import com.hms.annotated.sql.view.InnerJoin;
import com.hms.annotated.sql.view.LeftJoin;
import com.hms.annotated.sql.view.PrimaryJoin;

@ViewRecord(dbName = Series.MEDIA_TABLE)
public record MediaInfo(@PrimaryJoin MediaItem mediaItem,
        @LeftJoin(referencedClass = MediaItem.class) Movie movie,
        @LeftJoin(referencedClass = MediaItem.class) Episode episode,
        @LeftJoin(referencedClass = Episode.class) Season season,
        @LeftJoin(referencedClass = Episode.class) Series series) {

}
