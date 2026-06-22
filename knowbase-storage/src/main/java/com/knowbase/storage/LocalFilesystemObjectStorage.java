package com.knowbase.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalFilesystemObjectStorage implements ObjectStorage {

    private final Path root;
    private final Map<String, byte[]> inlineObjects = new ConcurrentHashMap<>();

    public LocalFilesystemObjectStorage(Path root) {
        this.root = root;
    }

    public LocalFilesystemObjectStorage() {
        this(Paths.get(System.getProperty("java.io.tmpdir"), "knowbase-storage"));
    }

    @Override
    public String storageType() {
        return "local-fs";
    }

    @Override
    public StoredObject put(String bucket, String objectKey, InputStream inputStream, String contentType) {
        try {
            Path target = root.resolve(bucket).resolve(objectKey);
            Files.createDirectories(target.getParent());
            byte[] bytes = inputStream.readAllBytes();
            Files.write(target, bytes);
            return new StoredObject(bucket, objectKey, "file://" + target, contentType, bytes.length);
        } catch (IOException exception) {
            throw new IllegalStateException("写入对象存储失败: " + bucket + "/" + objectKey, exception);
        }
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        if ("inline".equals(bucket)) {
            byte[] bytes = inlineObjects.get(objectKey);
            if (bytes == null) {
                throw new IllegalArgumentException("内联对象不存在: " + objectKey);
            }
            return new ByteArrayInputStream(bytes);
        }
        try {
            Path target = root.resolve(bucket).resolve(objectKey);
            return Files.newInputStream(target);
        } catch (IOException exception) {
            throw new IllegalStateException("读取对象存储失败: " + bucket + "/" + objectKey, exception);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        if ("inline".equals(bucket)) {
            inlineObjects.remove(objectKey);
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(bucket).resolve(objectKey));
        } catch (IOException exception) {
            throw new IllegalStateException("删除对象存储失败: " + bucket + "/" + objectKey, exception);
        }
    }

    public void putInline(String objectKey, byte[] content) {
        inlineObjects.put(objectKey, content);
    }
}
