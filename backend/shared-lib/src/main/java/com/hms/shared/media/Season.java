package com.hms.shared.media;

import java.util.List;

import com.hms.shared.dao.ChildKey;
import com.hms.shared.dao.ParentKey;
import com.hms.shared.dao.PrimaryKey;
import com.hms.shared.dao.SQLiteSerializable;

public record Season(@PrimaryKey String seasonId, @ParentKey(referencedClass = Series.class) String seriesId,
        String name, int seasonNumber, @ChildKey(referencedClass = Episode.class) List<Episode> episodes)
        implements SQLiteSerializable {

    @Override
    public String getDbPath() {
        return "media_records.db";
    }

    @Override
    public String getTableName() {
        return "seasons";
    }
}
