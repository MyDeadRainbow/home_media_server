package com.hms.shared.messaging.catalogupdates;

import com.hms.shared.media.MediaCategory;
import com.hms.shared.messaging.JsonSerializable;

public record CatalogUpdate(String mediaId, CatalogUpdateType updateType, String title, MediaCategory mediaType, Integer year, String description)
        implements JsonSerializable<CatalogUpdate> {
    
    public static final String TOPIC = "catalog-updates";
}
