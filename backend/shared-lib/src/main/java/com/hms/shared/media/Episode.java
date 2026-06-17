package com.hms.shared.media;

import com.hms.shared.dao.ParentKey;
import com.hms.shared.dao.PrimaryKey;
import com.hms.shared.dao.SQLiteSerializable;

public record Episode(@PrimaryKey String mediaId,
        @ParentKey(referencedClass = Season.class) String seasonId,
        @ParentKey(referencedClass = Series.class) String seriesId,
        String name, int episodeNumber, String filePath)
        implements SQLiteSerializable {

    @Override
    public String getDbPath() {
        return "media_records.db";
    }

    @Override
    public String getTableName() {
        return "episodes";
    }

}
