package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.smart.SmartTableDocumentChunker;
import com.knowbase.tokenizer.ApproximateTokenizer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartTableDocumentChunkerTest {

    private final SmartTableDocumentChunker chunker = new SmartTableDocumentChunker();

    @Test
    void producesOneChunkPerRowWhenStrategyIsTableRow() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        AMER,11,13
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        LibraryProfile libraryProfile = libraryProfile(512, 64);
        DocumentProfile profile = tableProfile(Map.of(
                "chunkEngine", "smart",
                "chunkingStrategy", "table_row",
                "tableRowGroupMaxRows", 1
        ));

        List<DocumentChunk> chunks = chunker.chunk(
                libraryProfile.libraryId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                parsed,
                libraryProfile,
                profile,
                new ApproximateTokenizer("approx-test", "1"),
                Map.of()
        );

        assertEquals(4, chunks.size());
        assertTrue(chunks.stream().allMatch(chunk -> "table_row".equals(chunk.chunkBoundaryType())));
        assertFalse(Boolean.TRUE.equals(chunks.getFirst().metadata().get("indexable")));
        assertEquals(3, chunks.stream().filter(chunk -> Boolean.TRUE.equals(chunk.metadata().get("indexable"))).count());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.content().contains("[Sheet:")));
    }

    @Test
    void mergesRowsUnderTokenWindowForTableRowTokenWindowStrategy() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.csv",
                "weekly.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        AMER,11,13
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        LibraryProfile libraryProfile = libraryProfile(512, 64);
        DocumentProfile profile = tableProfile(Map.of("chunkEngine", "smart"));

        List<DocumentChunk> chunks = chunker.chunk(
                libraryProfile.libraryId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                parsed,
                libraryProfile,
                profile,
                new ApproximateTokenizer("approx-test", "1"),
                Map.of()
        );

        assertTrue(chunks.size() < 4);
        assertTrue(chunks.stream().anyMatch(chunk -> "table_row_group".equals(chunk.chunkBoundaryType())));
    }

    @Test
    void marksSparseLayoutRowsNonIndexableOnWideSheets() {
        List<StructuralBlock> rows = List.of(
                block("A: 星图深海工作周报", 0, 12),
                block("A: 部门,B: 软件技术部,C: 姓名,D: 杜鹏飞,E: 汇报周期,F: 2026年5月06日", 1, 12),
                block("A: 项目,B: 工作内容,C: 完成情况", 2, 12),
                block("A: 1,B: FB项目,C: 配合三方测试,F: 杜鹏飞,I: 已完成", 3, 12)
        );
        ParsedDocument parsed = new ParsedDocument(
                "memory://weekly.xlsx",
                "weekly.xlsx",
                "",
                ContentFamily.STRUCTURED_TABLE,
                Map.of(),
                rows
        );
        List<DocumentChunk> chunks = chunker.chunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                parsed,
                libraryProfile(512, 64),
                tableProfile(Map.of(
                        "chunkEngine", "smart",
                        "chunkingStrategy", "table_row",
                        "tableRowGroupMaxRows", 1
                )),
                new ApproximateTokenizer("approx-test", "1"),
                Map.of()
        );

        assertEquals(4, chunks.size());
        assertFalse(Boolean.TRUE.equals(chunks.getFirst().metadata().get("indexable")));
        assertFalse(Boolean.TRUE.equals(chunks.get(2).metadata().get("indexable")));
        assertTrue(Boolean.TRUE.equals(chunks.get(3).metadata().get("indexable")));
        assertEquals(2, chunks.stream().filter(chunk -> Boolean.TRUE.equals(chunk.metadata().get("indexable"))).count());
    }

    @Test
    void shouldUseWhenChunkEngineSmartAndStructuredTable() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://t.csv",
                "t.csv",
                "",
                ContentFamily.STRUCTURED_TABLE,
                Map.of(),
                List.of()
        );
        assertTrue(SmartTableDocumentChunker.shouldUse(parsed, tableProfile(Map.of()), Map.of()));
        assertFalse(SmartTableDocumentChunker.shouldUse(
                parsed,
                tableProfile(Map.of()),
                Map.of("chunkEngine", "token")
        ));
    }

    private static StructuralBlock block(String content, int rowIndex, int columnCount) {
        List<String> keys = new ArrayList<>();
        for (int index = 0; index < columnCount; index++) {
            keys.add(String.valueOf((char) ('A' + index)));
        }
        return new StructuralBlock(
                "table_row",
                0,
                content,
                rowIndex,
                Map.of("sheetName", "周报3月", "rowIndex", rowIndex, "columnKeys", keys, "columnEnd", columnCount - 1)
        );
    }

    private static DocumentProfile tableProfile(Map<String, Object> options) {
        HashMap<String, Object> merged = new HashMap<>(options);
        merged.putIfAbsent("chunkingStrategy", "table_row_token_window");
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                String.valueOf(merged.get("chunkingStrategy")),
                null,
                Map.of(),
                Map.copyOf(merged),
                true
        );
    }

    private static LibraryProfile libraryProfile(int chunkMaxTokens, int chunkOverlapTokens) {
        UUID libraryId = UUID.randomUUID();
        return new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                chunkMaxTokens,
                chunkOverlapTokens,
                12,
                Map.of(),
                Instant.now()
        );
    }
}
