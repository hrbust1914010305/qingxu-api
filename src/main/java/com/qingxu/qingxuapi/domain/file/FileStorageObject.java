package com.qingxu.qingxuapi.domain.file;

public record FileStorageObject(
        String storageKey,
        String storagePath,
        String extension,
        long size,
        String checksum
) {
}
