package com.knowbase.library.service;

import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.dto.CleanupOrphanChunksResponse;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class ChunkProfileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ChunkProfileCleanupService.class);

    private final DocumentChunkMapper chunkMapper;
    private final DocMetadataStore docMetadataStore;
    private final LibraryConfigResolver libraryConfigResolver;

    public ChunkProfileCleanupService(
            DocumentChunkMapper chunkMapper,
            DocMetadataStore docMetadataStore,
            LibraryConfigResolver libraryConfigResolver) {
        this.chunkMapper = chunkMapper;
        this.docMetadataStore = docMetadataStore;
        this.libraryConfigResolver = libraryConfigResolver;
    }

    public CleanupOrphanChunksResponse cleanupOrphanNonPrimaryChunks(UUID libraryId, String tenantId) {
        libraryConfigResolver.requireLibrary(libraryId);
        String primary = primaryProfileId(libraryId);
        if (!StringUtils.hasText(primary)) {
            return new CleanupOrphanChunksResponse(0, 0);
        }
        String tid = tenantId.trim();
        List<String> chunkProfiles = chunkMapper.findDistinctChunkProfileIds(libraryId, tid);
        int removedChunks = 0;
        int cleanedProfiles = 0;
        for (String profileId : chunkProfiles) {
            if (primary.equals(profileId)) {
                continue;
            }
            if (docMetadataStore.existsChunkProfileId(libraryId, profileId)) {
                continue;
            }
            int deleted = chunkMapper.deleteOrphanChunksForProfile(libraryId, tid, profileId);
            if (deleted > 0) {
                removedChunks += deleted;
                cleanedProfiles++;
                log.info(
                        "Cleaned {} orphan chunks for empty profile {} in library {}",
                        deleted,
                        profileId,
                        libraryId);
            }
        }
        return new CleanupOrphanChunksResponse(removedChunks, cleanedProfiles);
    }

    private String primaryProfileId(UUID libraryId) {
        VectorLibraryConfig cfg = libraryConfigResolver.config(libraryId);
        String primary = cfg.getPrimaryChunkProfileId();
        return primary != null ? primary.trim() : "";
    }
}
