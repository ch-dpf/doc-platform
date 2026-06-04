package com.docplatform.ingest.storage;

import com.docplatform.ingest.config.StorageProperties;
import com.docplatform.ingest.domain.DocMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public class LocalFsDocumentObjectStorage implements DocumentObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFsDocumentObjectStorage.class);

    private final StorageProperties storageProperties;
    private final Path basePath;

    public LocalFsDocumentObjectStorage(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.basePath = Path.of(storageProperties.getLocal().getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new StorageException("Cannot create local storage directory: " + basePath, e);
        }
        log.info("Local FS document storage enabled at {}", basePath);
    }

    @Override
    public String type() {
        return "local-fs";
    }

    @Override
    public void putObject(String objectKey, InputStream stream, long size, String contentType) {
        Path target = resolvePath(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + target, e);
        }
    }

    @Override
    public String readAsString(String objectKey) {
        Path path = resolvePath(objectKey);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + path, e);
        }
    }

    @Override
    public void removeDocumentArtifacts(DocMetadata doc) {
        removeObjectIfPresent(doc.getStorageKey());
        removeObjectIfPresent(doc.getParsedTextKey());
        removeByPrefix(doc.getTenantId() + "/" + doc.getDocId() + "/");
    }

    @Override
    public void removeObjectIfPresent(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        Path path = resolvePath(objectKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new StorageException("Failed to remove file: " + path, e);
        }
    }

    @Override
    public int removeByPrefix(String prefix) {
        Path root = resolvePath(prefix);
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            var paths = walk.sorted(Comparator.reverseOrder()).toList();
            int removed = 0;
            for (Path p : paths) {
                if (Files.deleteIfExists(p)) {
                    removed++;
                }
            }
            return removed;
        } catch (IOException e) {
            throw new StorageException("Failed to remove prefix: " + root, e);
        }
    }

    @Override
    public String resolveAccessUrl(String objectKey) {
        return resolvePath(objectKey).toUri().toString();
    }

    private Path resolvePath(String objectKey) {
        String key = storageProperties.normalizeObjectKey(objectKey);
        Path resolved = basePath.resolve(key.replace('\\', '/')).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new StorageException("Invalid object key (path traversal): " + objectKey);
        }
        return resolved;
    }
}
