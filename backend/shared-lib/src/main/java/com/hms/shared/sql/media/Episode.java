package com.hms.shared.sql.media;

import com.hms.shared.sql.ParentLink;
import com.hms.shared.sql.PrimaryKey;
import com.hms.shared.sql.SqlRecord;

@SqlRecord(dbName = Series.MEDIA_TABLE)
public record Episode(
        @PrimaryKey String episodeId,
        @ParentLink String seasonId,
        @ParentLink String seriesId,
        @ParentLink MediaItem media,
        String name,
        int episodeNumber) {

}
