package com.knowbase.vector.service;

import com.knowbase.event.DocumentReadyForIndexEvent;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.dto.RebuildLibraryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryRebuildService {

    private static final Logger log = LoggerFactory.getLogger(LibraryRebuildService.class);

    private final DocMetadataStore docMetadataStore;
    private final IndexingService indexingService;

    public LibraryRebuildService(DocMetadataStore docMetadataStore, IndexingService indexingService) {
        this.docMetadataStore = docMetadataStore;
        this.indexingService = indexingService;
    }

    public int countCandidates(UUID libraryId, String tenantId) {
        return findCandidates(libraryId, tenantId).size();
    }

    public RebuildLibraryResponse scheduleRebuild(UUID libraryId, String tenantId) {
        int count = countCandidates(libraryId, tenantId);
        if (count > 0) {
            rebuildAllAsync(libraryId, tenantId.trim());
        }
        String message = count > 0
                ? "已提交 " + count + " 个文档的批量重索引任务（异步）"
                : "当前库内没有可重索引的已解析文档";
        return new RebuildLibraryResponse(count, message);
    }

    @Async
    public void rebuildAllAsync(UUID libraryId, String tenantId) {
        List<DocMetadata> docs = findCandidates(libraryId, tenantId);
        log.info("Starting library rebuild for {} tenant {} ({} docs)", libraryId, tenantId, docs.size());
        for (DocMetadata doc : docs) {
            try {
                rebuildFromStored(doc);
            } catch (Exception e) {
                log.error("Rebuild failed for doc {} v{}: {}", doc.getDocId(), doc.getVersion(), e.getMessage(), e);
            }
        }
        log.info("Library rebuild finished for {} tenant {}", libraryId, tenantId);
    }

    private List<DocMetadata> findCandidates(UUID libraryId, String tenantId) {
        return docMetadataStore.findParsedWithTextKey(libraryId, tenantId.trim());
    }

    private void rebuildFromStored(DocMetadata doc) {
        String parsedKey = doc.getParsedTextKey();
        if (!StringUtils.hasText(parsedKey)) {
            throw new IllegalStateException("Missing parsed text key for doc " + doc.getDocId());
        }
        if (doc.getParseStatus() != ParseStatus.PARSED) {
            throw new IllegalStateException("Doc " + doc.getDocId() + " is not PARSED");
        }
        indexingService.index(DocumentReadyForIndexEvent.create(
                doc.getLibraryId(),
                doc.getDocId(),
                doc.getTenantId(),
                doc.getVersion(),
                doc.getChecksumSha256() != null ? doc.getChecksumSha256() : "",
                doc.getMimeType() != null ? doc.getMimeType() : "text/plain",
                null,
                parsedKey.trim()));
    }
}
