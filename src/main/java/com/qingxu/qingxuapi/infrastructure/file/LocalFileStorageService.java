package com.qingxu.qingxuapi.infrastructure.file;

import com.qingxu.qingxuapi.common.config.QingxuFileProperties;
import com.qingxu.qingxuapi.domain.file.FileStorageObject;
import com.qingxu.qingxuapi.domain.file.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path localRoot;
    private final Path chunkRoot;

    public LocalFileStorageService(QingxuFileProperties properties) {
        this.localRoot = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        this.chunkRoot = Path.of(properties.getChunkRoot()).toAbsolutePath().normalize();
    }

    @Override
    public FileStorageObject store(MultipartFile file, String bizType, String extension) throws IOException {
        String storageKey = UUID.randomUUID().toString().replace("-", "");
        Path target = resolveFileTarget(bizType, extension, storageKey);
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        file.transferTo(temp);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return new FileStorageObject(storageKey, localRoot.relativize(target).toString().replace('\\', '/'), extension, Files.size(target), sha256(target));
    }

    @Override
    public String storeChunk(String uploadId, Integer chunkIndex, MultipartFile chunk) throws IOException {
        Path directory = safeResolve(chunkRoot, uploadId);
        Files.createDirectories(directory);
        Path target = safeResolve(directory, "chunk-" + chunkIndex + ".part");
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        chunk.transferTo(temp);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return chunkRoot.relativize(target).toString().replace('\\', '/');
    }

    @Override
    public InputStream load(String storagePath) throws IOException {
        return Files.newInputStream(safeResolve(localRoot, storagePath));
    }

    @Override
    public FileStorageObject mergeChunks(String uploadId, int totalChunks, String bizType, String extension) throws IOException {
        String storageKey = UUID.randomUUID().toString().replace("-", "");
        Path target = resolveFileTarget(bizType, extension, storageKey);
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunk = safeResolve(chunkRoot, uploadId + "/chunk-" + i + ".part");
                if (!Files.exists(chunk)) {
                    throw new IOException("Missing chunk " + i);
                }
                Files.copy(chunk, output);
            }
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return new FileStorageObject(storageKey, localRoot.relativize(target).toString().replace('\\', '/'), extension, Files.size(target), sha256(target));
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Files.deleteIfExists(safeResolve(localRoot, storagePath));
    }

    @Override
    public void deleteChunks(String uploadId) throws IOException {
        Path directory = safeResolve(chunkRoot, uploadId);
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private Path resolveFileTarget(String bizType, String extension, String storageKey) {
        String safeBizType = (bizType == null || bizType.isBlank()) ? "default" : bizType;
        LocalDate date = LocalDate.now();
        String fileName = extension == null || extension.isBlank() ? storageKey : storageKey + "." + extension;
        return safeResolve(localRoot, safeBizType + "/" + date.getYear() + "/" + twoDigits(date.getMonthValue()) + "/" + twoDigits(date.getDayOfMonth()) + "/" + fileName);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static Path safeResolve(Path root, String child) {
        Path resolved = root.resolve(child).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes configured root");
        }
        return resolved;
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path); DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
