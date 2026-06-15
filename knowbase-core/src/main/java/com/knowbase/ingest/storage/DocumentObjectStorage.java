package com.knowbase.ingest.storage;

import com.knowbase.ingest.domain.DocMetadata;

import java.io.InputStream;

/**
 * 非结构化文档对象存储（MinIO / 本地文件系统等实现可切换）。
 */
public interface DocumentObjectStorage {

    String type();

    void putObject(String objectKey, InputStream stream, long size, String contentType);

    String readAsString(String objectKey);

    void removeObjectIfPresent(String objectKey);

    int removeByPrefix(String prefix);

    void removeDocumentArtifacts(DocMetadata doc);

    /** 供索引流水线使用的可读 URL；本地 FS 时可能为 file URI，实际读取优先走 object key。 */
    String resolveAccessUrl(String objectKey);
}
