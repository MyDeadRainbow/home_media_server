package com.hms.shared.messaging.catalogupdates;

import java.util.List;

import com.hms.shared.media.MediaCategory;
import com.hms.shared.messaging.JsonSerializable;
import com.hms.shared.messaging.Topics;

public record CatalogUpdate(CatalogUpdateType updateType, MediaCategory mediaType, List<FilePathRecord> filePaths)
        implements JsonSerializable<CatalogUpdate> {
    
    public static final String TOPIC = Topics.CATALOG_UPDATES;

    
}
