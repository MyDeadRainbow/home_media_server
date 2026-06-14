package com.hms.acquisition.importmedia;

import com.hms.shared.dao.PrimaryKey;
import com.hms.shared.dao.SQLiteSerializable;

public record ImportMediaEntry(@PrimaryKey String id, String title, ImportMediaStatus status, String magnetLink)
        implements SQLiteSerializable {

    @Override
    public String getDbPath() {
        return "media_requests.db";
    }

    @Override
    public String getTableName() {
        return "media_requests";
    }

}
