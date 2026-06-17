package com.hms.stream.importmedia;

import com.hms.shared.media.MediaCategory;

public record ImportMediaRequest(String title, String magnetLink, MediaCategory category) {
    
}
