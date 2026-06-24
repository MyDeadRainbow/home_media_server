package com.hms.annotated.sql.media;

import com.hms.annotated.sql.PrimaryKey;
import com.hms.annotated.sql.TableRecord;

@TableRecord(dbName = Series.MEDIA_TABLE)
public record MediaItem(@PrimaryKey String mediaId, String filePath) {

}
