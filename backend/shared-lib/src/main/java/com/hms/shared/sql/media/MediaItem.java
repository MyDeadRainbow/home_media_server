package com.hms.shared.sql.media;

import com.hms.shared.sql.PrimaryKey;
import com.hms.shared.sql.SqlRecord;

@SqlRecord(dbName = Series.MEDIA_TABLE)
public record MediaItem(@PrimaryKey String mediaId, String filePath) {

}
