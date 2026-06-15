package com.knowbase.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.mapper.DocMetadataMapper;
import com.knowbase.ingest.storage.ObjectStorageService;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.library.dto.DeleteVectorLibraryResponse;
import com.knowbase.library.mapper.VectorLibraryMapper;
import com.knowbase.vector.domain.DocumentIndexJob;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import com.knowbase.vector.mapper.DocumentIndexJobMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryDeletionService {

    private static final Logger log = LoggerFactory.getLogger(LibraryDeletionService.class);

    private final VectorLibraryMapper libraryMapper;
    private final DocMetadataMapper docMetadataMapper;
    private final ObjectStorageService storageService;
    private final DocumentChunkMapper chunkMapper;
    private final DocumentIndexJobMapper indexJobMapper;

    public LibraryDeletionService(
            VectorLibraryMapper libraryMapper,
            DocMetadataMapper docMetadataMapper,
            ObjectStorageService storageService,
            DocumentChunkMapper chunkMapper,
            DocumentIndexJobMapper indexJobMapper) {
        this.libraryMapper = libraryMapper;
        this.docMetadataMapper = docMetadataMapper;
        this.storageService = storageService;
        this.chunkMapper = chunkMapper;
        this.indexJobMapper = indexJobMapper;
    }

    @KnowbaseTransactional
    public DeleteVectorLibraryResponse delete(UUID libraryId, String tenantId) {
        if (VectorLibraryService.DEFAULT_LIBRARY_ID.equals(libraryId)) {
            throw new LibraryNotDeletableException(libraryId, "系统默认知识库不可删除");
        }
        VectorLibrary lib = libraryMapper.selectById(libraryId);
        if (lib == null) {
            throw new LibraryNotFoundException(libraryId);
        }
        if (tenantId != null && !tenantId.isBlank() && !lib.getTenantId().equals(tenantId.trim())) {
            throw new LibraryNotDeletableException(libraryId, "租户不匹配");
        }

        List<DocMetadata> docs = docMetadataMapper.selectList(
                new LambdaQueryWrapper<DocMetadata>().eq(DocMetadata::getLibraryId, libraryId));
        int docCount = docs.size();
        for (DocMetadata doc : docs) {
            try {
                storageService.removeDocumentArtifacts(doc);
            } catch (Exception e) {
                log.warn("Failed to remove storage for doc {}: {}", doc.getDocId(), e.getMessage());
            }
        }

        int chunkCount = chunkMapper.countByLibraryId(libraryId);
        chunkMapper.deleteByLibraryId(libraryId);
        indexJobMapper.delete(new LambdaQueryWrapper<DocumentIndexJob>()
                .eq(DocumentIndexJob::getLibraryId, libraryId));
        docMetadataMapper.delete(new LambdaQueryWrapper<DocMetadata>().eq(DocMetadata::getLibraryId, libraryId));

        String prefix = lib.getTenantId() + "/" + libraryId + "/";
        try {
            storageService.removeByPrefix(prefix);
        } catch (Exception e) {
            log.warn("Failed to remove storage prefix {}: {}", prefix, e.getMessage());
        }

        libraryMapper.deleteById(libraryId);
        log.info("Deleted library {} ({}) docs={} chunks={}", libraryId, lib.getName(), docCount, chunkCount);

        return new DeleteVectorLibraryResponse(
                libraryId,
                lib.getName(),
                docCount,
                chunkCount,
                "知识库已删除");
    }
}
