package com.hms.catalog;

import java.util.List;

import com.hms.shared.dao.PrimaryKey;
import com.hms.shared.dao.SQLiteSerializable;

public record MediaItem(
        @PrimaryKey
        String id,
        String title,
        String type,
        Integer year,
        String description
        // ,
        // String posterUrl,
        // String streamUrl,
        // List<String> subtitleLanguages
) implements SQLiteSerializable {
        @Override
        public String getDbPath() {
                return "media_catalog.db";
        }
        
        @Override
        public String getTableName() {
                return "media_items";
        }
}
