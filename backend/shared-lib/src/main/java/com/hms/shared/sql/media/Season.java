package com.hms.shared.sql.media;

import java.util.List;

import com.hms.shared.sql.ChildLink;
import com.hms.shared.sql.ForeignKey;
import com.hms.shared.sql.PrimaryKey;
import com.hms.shared.sql.SqlRecord;

@SqlRecord(dbName = Series.MEDIA_TABLE)
public record Season(
        @PrimaryKey String seasonId,
        @ForeignKey(referencedClass = Series.class) String seriesId,
        String name,
        int seasonNumber,
        @ChildLink List<Episode> episodes) {

}
