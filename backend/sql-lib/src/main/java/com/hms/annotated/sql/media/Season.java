package com.hms.annotated.sql.media;

import java.util.List;

import com.hms.annotated.sql.ManyToOne;
import com.hms.annotated.sql.OneToMany;
import com.hms.annotated.sql.PrimaryKey;
import com.hms.annotated.sql.TableRecord;

@TableRecord(dbName = Series.MEDIA_TABLE)
public record Season(
        @PrimaryKey String seasonId,
        @ManyToOne(toOne = Series.class) String seriesId,
        String name,
        int seasonNumber,
        @OneToMany List<Episode> episodes) {

}
