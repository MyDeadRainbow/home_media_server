package com.hms.stream;

import com.hms.shared.dao.PrimaryKey;
import com.hms.shared.dao.SQLiteSerializable;
import com.hms.shared.messaging.JsonSerializable;

public record MediaRecord(@PrimaryKey String mediaId, String filePath)
        implements JsonSerializable<MediaRecord>, SQLiteSerializable {

    @Override
    public String getDbPath() {
        return "media_records.db";
    }

    @Override
    public String getTableName() {
        return "media_records";
    }
}
