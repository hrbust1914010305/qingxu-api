package com.qingxu.qingxuapi.interfaces.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FileUploadInitRequest(
        @NotBlank String fileName,
        String mimeType,
        @NotNull @Positive Long size,
        @Positive Long chunkSize,
        @NotBlank String fingerprint,
        String bizType
) {
}
