package com.knowbase.vector.service;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.dto.TemporalMetadataBackfillResponse;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.vector.dto.DocumentChunkBackfillRow;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import com.knowbase.vector.retrieval.ChunkMetadataBuilder;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;

import java.util.List;
import java.util.UUID;

/** 为存量分块回填 period/submitter 等时间元数据。 */
@Service
public class TemporalMetadataBackfillService {

    private final LibraryConfigResolver libraryConfigResolver;
    private final DocMetadataStore docMetadataStore;
    private final DocumentChunkMapper chunkMapper;

    public TemporalMetadataBackfillService(
            LibraryConfigResolver libraryConfigResolver,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        this.libraryConfigResolver = libraryConfigResolver;
        this.docMetadataStore = docMetadataStore;
        this.chunkMapper = chunkMapper;
    }

    @KnowbaseTransactional
    public TemporalMetadataBackfillResponse backfillLibrary(UUID libraryId, String tenantId) {
        var library = libraryConfigResolver.requireLibrary(libraryId);
        String effectiveTenant = tenantId != null && !tenantId.isBlank()
                ? tenantId.strip()
                : library.getTenantId();
        List<DocMetadata> docs = docMetadataStore.findActiveByLibrary(libraryId, effectiveTenant);
        int processedDocs = 0;
        int updatedChunks = 0;
        int skippedChunks = 0;
        for (DocMetadata doc : docs) {
            int chunkCount = chunkMapper.countByDocIdAndVersion(doc.getDocId(), doc.getVersion());
            if (chunkCount <= 0) {
                continue;
            }
            processedDocs++;
            List<DocumentChunkBackfillRow> chunks =
                    chunkMapper.listChunksForTemporalBackfill(doc.getDocId(), doc.getVersion());
            for (DocumentChunkBackfillRow chunk : chunks) {
                if (ChunkMetadataBuilder.hasCompleteTemporalFields(chunk.metadataJson())) {
                    skippedChunks++;
                    continue;
                }
                String merged = ChunkMetadataBuilder.mergeTemporalIntoExisting(
                        chunk.metadataJson(), doc, chunk.content());
                if (merged == null || merged.equals(chunk.metadataJson())) {
                    skippedChunks++;
                    continue;
                }
                chunkMapper.updateChunkMetadata(chunk.chunkId(), merged);
                updatedChunks++;
            }
        }
        return new TemporalMetadataBackfillResponse(processedDocs, updatedChunks, skippedChunks);
    }
}
