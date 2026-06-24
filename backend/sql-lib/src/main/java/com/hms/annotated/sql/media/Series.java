package com.hms.annotated.sql.media;

import java.util.List;

import com.hms.annotated.sql.OneToMany;
import com.hms.annotated.sql.PrimaryKey;
import com.hms.annotated.sql.TableRecord;

@TableRecord(dbName = Series.MEDIA_TABLE)
public record Series(@PrimaryKey String seriesId, String name,
        @OneToMany List<Season> seasons, 
        @OneToMany List<Episode> episodes) {

    public static final String MEDIA_TABLE = "media.db";
}
