package com.hms.shared.sql.media;

import java.util.List;

import com.hms.shared.sql.ChildLink;
import com.hms.shared.sql.PrimaryKey;
import com.hms.shared.sql.SqlRecord;

@SqlRecord(dbName = Series.MEDIA_TABLE)
public record Series(@PrimaryKey String seriesId, String name,
        @ChildLink List<Season> seasons) {

    public static final String MEDIA_TABLE = "media.db";
}
