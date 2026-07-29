package com.hms.shared.json;

import com.hms.shared.media.MediaCategory;
import com.hms.shared.messaging.JsonSerializable;

public record ImportMediaRequest(String title, String magnetLink, MediaCategory category) implements JsonSerializable {
    
}
