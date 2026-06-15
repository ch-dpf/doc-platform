package com.knowbase.vector.retrieval;

import com.knowbase.ingest.domain.DocMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkMetadataBuilderBackfillTest {

    @Test
    void mergeTemporalIntoExistingPreservesOtherFields() {
        DocMetadata doc = new DocMetadata();
        doc.setFileName("2025/杜鹏飞-周报（9.15-9.19）.xlsx");
        doc.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String existing = "{\"chunkProfileId\":\"cp_test\",\"mimeType\":\"application/pdf\"}";
        String chunk = "【杜鹏飞·工作周报·2025年9月15日--9月19日】\n\n1\t海图项目\t开发接口\t\t2025.9.19\t杜鹏飞\t完成开发任务\t\t已完成";

        String merged = ChunkMetadataBuilder.mergeTemporalIntoExisting(existing, doc, chunk);

        assertTrue(merged.contains("\"chunkProfileId\":\"cp_test\""));
        assertTrue(merged.contains("\"periodYear\":\"2025\""));
        assertTrue(merged.contains("\"submitter\":\"杜鹏飞\""));
    }

    @Test
    void hasCompleteTemporalFieldsDetectsBackfilledMetadata() {
        String json = "{\"periodYear\":\"2025\",\"periodStart\":\"2025-09-01\",\"submitter\":\"杜鹏飞\"}";
        assertTrue(ChunkMetadataBuilder.hasCompleteTemporalFields(json));
        assertFalse(ChunkMetadataBuilder.hasCompleteTemporalFields("{\"periodYear\":\"2025\"}"));
    }
}
