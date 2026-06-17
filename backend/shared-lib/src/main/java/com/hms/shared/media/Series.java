package com.hms.shared.media;

import java.util.List;

import com.hms.shared.dao.ChildKey;
import com.hms.shared.dao.SQLiteSerializable;

public record Series(String seriesId, String name, @ChildKey(referencedClass = Season.class) List<Season> seasons) implements SQLiteSerializable {

    @Override
    public String getDbPath() {
        return "media_records.db";
    }

    @Override
    public String getTableName() {
        return "series";
    }}
