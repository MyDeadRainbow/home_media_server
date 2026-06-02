package com.hms.acquisition;

import jakarta.validation.constraints.NotBlank;

public record ImportMediaRequest(
        @NotBlank String title,
        @NotBlank String type,
        String quality
) {
}
