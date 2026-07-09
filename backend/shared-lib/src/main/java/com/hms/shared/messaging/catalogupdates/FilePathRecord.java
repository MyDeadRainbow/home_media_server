package com.hms.shared.messaging.catalogupdates;

import com.hms.shared.messaging.JsonSerializable;

public record FilePathRecord(String mediaId, String filePath) implements JsonSerializable {
    
}
