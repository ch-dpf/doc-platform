package com.knowbase.library.service;

import com.knowbase.ingest.service.InvalidDocumentException;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.CapacityLimitsSettings;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LibraryCapacityValidator {

    private final LibraryConfigResolver libraryConfigResolver;
    private final DocMetadataStore metadataStore;
    private final DocumentChunkMapper chunkMapper;

    public LibraryCapacityValidator(
            LibraryConfigResolver libraryConfigResolver,
            DocMetadataStore metadataStore,
            DocumentChunkMapper chunkMapper) {
        this.libraryConfigResolver = libraryConfigResolver;
        this.metadataStore = metadataStore;
        this.chunkMapper = chunkMapper;
    }

    public void requireNewDocument(UUID libraryId, long fileSizeBytes, String fileName) {
        CapacityLimitsSettings limits = capacityLimits(libraryId);
        if (limits.getMaxDocuments() > 0) {
            int activeDocs = metadataStore.countActiveByLibraryId(libraryId);
            if (activeDocs >= limits.getMaxDocuments()) {
                throw InvalidDocumentException.libraryDocumentLimit(fileName, activeDocs, limits.getMaxDocuments());
            }
        }
        requireTotalSizeAfterDelta(libraryId, fileName, fileSizeBytes);
    }

    /** 覆盖当前版本：库内总大小按「减去旧版、加上新版」估算。 */
    public void requireReplaceDocument(UUID libraryId, long previousSizeBytes, long newSizeBytes, String fileName) {
        long delta = newSizeBytes - previousSizeBytes;
        if (delta <= 0) {
            return;
        }
        requireTotalSizeAfterDelta(libraryId, fileName, delta);
    }

    /** 保留历史版本：新版本对象额外占用存储。 */
    public void requireAdditionalVersionStorage(UUID libraryId, long additionalBytes, String fileName) {
        if (additionalBytes <= 0) {
            return;
        }
        requireTotalSizeAfterDelta(libraryId, fileName, additionalBytes);
    }

    public void requireChunkCapacity(UUID libraryId, int replacingChunks, int newChunks) {
        CapacityLimitsSettings limits = capacityLimits(libraryId);
        if (limits.getMaxChunkEntries() <= 0) {
            return;
        }
        int current = chunkMapper.countByLibraryId(libraryId);
        long projected = (long) current - replacingChunks + newChunks;
        if (projected > limits.getMaxChunkEntries()) {
            throw new LibraryCapacityExceededException(
                    LibraryCapacityExceededException.CODE_CHUNK_LIMIT,
                    String.format(
                            "向量条目将达到 %d，超过库上限 %d。",
                            projected,
                            limits.getMaxChunkEntries()));
        }
    }

    private void requireTotalSizeAfterDelta(UUID libraryId, String fileName, long sizeDeltaBytes) {
        CapacityLimitsSettings limits = capacityLimits(libraryId);
        if (limits.getMaxTotalSizeBytes() <= 0) {
            return;
        }
        long total = metadataStore.sumSizeBytesByLibraryId(libraryId);
        long projected = total + sizeDeltaBytes;
        if (projected > limits.getMaxTotalSizeBytes()) {
            throw InvalidDocumentException.librarySizeLimit(
                    fileName, projected, limits.getMaxTotalSizeBytes());
        }
    }

    private CapacityLimitsSettings capacityLimits(UUID libraryId) {
        return libraryConfigResolver.capacityLimitsFor(libraryId);
    }
}
