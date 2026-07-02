package com.knowbase.application.service;

import com.knowbase.api.command.CreateRetrievalEvalSampleCommand;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.status.DocumentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.knowbase.application.service.RetrievalEvalDraftSupport.DEFAULT_ENABLED_DRAFTS;
import static com.knowbase.application.service.RetrievalEvalDraftSupport.MAX_DRAFTS_PER_GENERATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalEvalDraftGeneratorTest {

    private final RetrievalEvalDraftGenerator generator = new RetrievalEvalDraftGenerator();

    @Test
    void generatesDisabledDraftBoundToDocumentId() {
        UUID documentId = UUID.randomUUID();
        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "minio://bucket/开发目的和技术特点.docx",
                "开发目的和技术特点.docx",
                DocumentStatus.INDEXED,
                null,
                "hash",
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );
        DocumentChunk chunk = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                document.libraryId(),
                document.indexVersionId(),
                "本系统开发目的是提升浮标运维效率，技术特点包括实时采集与异常分析能力。",
                48,
                "tok",
                "1",
                "bge-m3",
                "token_window",
                null,
                Map.of("sectionTitle", "开发目的")
        );
        List<CreateRetrievalEvalSampleCommand> drafts = generator.generate(document, List.of(chunk), 8);
        assertEquals(1, drafts.size());
        CreateRetrievalEvalSampleCommand draft = drafts.getFirst();
        assertEquals(documentId, draft.expectedDocumentIds().getFirst());
        assertFalse(draft.enabled());
        assertTrue(draft.notes().startsWith(RetrievalEvalDraftSupport.NOTE_PREFIX));
        assertTrue(draft.question().contains("开发目的"));
    }

    @Test
    void skipsTitleOnlyChunksForManual() {
        UUID documentId = UUID.randomUUID();
        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "minio://bucket/浮标运维与资产管理系统用户使用手册.docx",
                "浮标运维与资产管理系统用户使用手册.docx",
                DocumentStatus.INDEXED,
                null,
                "hash",
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );
        DocumentChunk titleOnly = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                document.libraryId(),
                document.indexVersionId(),
                "一、系统概述",
                6,
                "tok",
                "1",
                "bge-m3",
                "token_window",
                null,
                Map.of("sectionTitle", "一、系统概述")
        );
        DocumentChunk body = new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                document.libraryId(),
                document.indexVersionId(),
                "系统概述：浮标运维与资产管理系统用于统一管理海上浮标设备，支持状态监测、巡检计划与告警闭环。",
                42,
                "tok",
                "1",
                "bge-m3",
                "token_window",
                null,
                Map.of("sectionTitle", "一、系统概述")
        );
        List<CreateRetrievalEvalSampleCommand> drafts = generator.generate(document, List.of(titleOnly, body), 10);
        assertEquals(1, drafts.size());
        assertTrue(drafts.getFirst().groundTruthContexts().getFirst().contains("统一管理"));
    }

    @Test
    void enabledByDefaultOnlyForFirstFiveInBatch() {
        assertTrue(RetrievalEvalDraftSupport.isEnabledByDefault(0));
        assertTrue(RetrievalEvalDraftSupport.isEnabledByDefault(4));
        assertFalse(RetrievalEvalDraftSupport.isEnabledByDefault(5));
        assertEquals(20, MAX_DRAFTS_PER_GENERATION);
        assertEquals(5, DEFAULT_ENABLED_DRAFTS);
    }

    @Test
    void identifiesTitleOnlyChunk() {
        DocumentChunk chunk = new DocumentChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "二、系统部署",
                6,
                "tok",
                "1",
                "bge-m3",
                "token_window",
                null,
                Map.of("sectionTitle", "二、系统部署")
        );
        assertTrue(RetrievalEvalDraftGenerator.isTitleOnlyChunk(chunk));
    }
}
