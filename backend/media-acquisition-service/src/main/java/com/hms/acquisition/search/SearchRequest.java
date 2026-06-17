package com.hms.acquisition.search;

import com.hms.shared.media.MediaCategory;

public record SearchRequest(String query, MediaCategory category) {
    
}
