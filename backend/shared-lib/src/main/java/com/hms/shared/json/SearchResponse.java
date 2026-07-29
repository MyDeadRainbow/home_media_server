package com.hms.shared.json;

import com.hms.shared.messaging.JsonSerializable;

public record SearchResponse(String title, String magnetLink, String source, String sourceUrl, String size,
        String seeders, String leechers) implements JsonSerializable {

}
