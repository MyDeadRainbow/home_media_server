package com.hms.catalog.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMediaRequest(
        @NotBlank String title,
        @NotBlank String type,
        @NotNull Integer year,
        String description,
        String posterUrl,
        String streamUrl
) {
}
