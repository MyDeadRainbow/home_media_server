package com.hms.annotated.sql.media;

import com.hms.annotated.sql.OneToOne;
import com.hms.annotated.sql.PrimaryKey;
import com.hms.annotated.sql.TableRecord;

@TableRecord(dbName = Series.MEDIA_TABLE)
public record Movie(@PrimaryKey String movieId, String name, @OneToOne MediaItem mediaItem) {

}
