package com.hms.annotated.sql.media;

import com.hms.annotated.sql.ManyToOne;
import com.hms.annotated.sql.OneToOne;
import com.hms.annotated.sql.PrimaryKey;
import com.hms.annotated.sql.TableRecord;

@TableRecord(dbName = Series.MEDIA_TABLE)
public record Episode(
        @PrimaryKey String episodeId,
        @ManyToOne(toOne = Season.class) String seasonId,
        @ManyToOne(toOne = Series.class) String seriesId,
        String name,
        int episodeNumber,
        @OneToOne MediaItem mediaItem) {

}
