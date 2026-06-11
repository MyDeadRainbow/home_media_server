package com.hms.shared.messaging.catalogupdates;

import com.google.gson.JsonObject;
import com.hms.shared.media.MediaCategory;
import com.hms.shared.messaging.JsonSerializable;

public record CatalogUpdate(String mediaId, CatalogUpdateType updateType, String title, MediaCategory mediaType, Integer year, String description)
        implements JsonSerializable<CatalogUpdate> {
    
    public static final String TOPIC = "catalog-updates";

    public CatalogUpdate(String mediaId, String updateType, String title, String mediaType, Integer year, String description) {
        this(mediaId, CatalogUpdateType.valueOf(updateType), title, MediaCategory.valueOf(mediaType), year, description);
    }
}
