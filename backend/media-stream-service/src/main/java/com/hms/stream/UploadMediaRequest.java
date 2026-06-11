package com.hms.stream;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;

public record UploadMediaRequest(
        MultipartFile file,
        @NotBlank String title,
        @NotBlank String type,
        Integer year,
        String description
) {
}
