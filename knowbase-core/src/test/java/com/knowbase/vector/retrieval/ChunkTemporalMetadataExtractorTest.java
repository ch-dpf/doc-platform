package com.knowbase.vector.retrieval;

import com.knowbase.ingest.domain.DocMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkTemporalMetadataExtractorTest {

    @Test
    void extractsPeriodMetadataFromWeeklyChunk() {
        DocMetadata doc = new DocMetadata();
        doc.setFileName("2025/杜鹏飞-周报（9.15-9.19）.xlsx");
        String chunk = """
                【杜鹏飞·工作周报·2025年9月15日--9月19日】
                列：序号|类别|工作内容|计划完成时间|责任人|执行要求|执行情况|说明

                1\t海图项目\t开发服务订阅的消息触发、新门户的后台接口\t\t2025.9.19\t杜鹏飞\t完成开发任务\t\t已完成""";

        ChunkTemporalMetadataExtractor.TemporalMetadata metadata =
                ChunkTemporalMetadataExtractor.extract(doc, chunk);

        assertEquals("2025", metadata.periodYear());
        assertEquals("2025-09-15", metadata.periodStart());
        assertEquals("2025-09-19", metadata.periodEnd());
        assertEquals("9", metadata.periodMonths());
        assertEquals("杜鹏飞", metadata.submitter());
        assertEquals("工作周报", metadata.sectionLabel());
        assertEquals("true", metadata.hasCompletedWork());
    }

    @Test
    void buildJsonIncludesTemporalFields() {
        DocMetadata doc = new DocMetadata();
        doc.setFileName("2025/杜鹏飞-周报（9.8-9.12）.xlsx");
        doc.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String chunk = "【杜鹏飞·工作周报·2025年9月8日--9月12日】\n\n1\t海图项目\t培训\t\t2025.9.12\t杜鹏飞\t完成系统培训\t\t已完成";

        String json = ChunkMetadataBuilder.buildJson(doc, new com.knowbase.pipeline.chunk.PipelineChunk(chunk, null, 0));

        assertTrue(json.contains("\"periodYear\":\"2025\""));
        assertTrue(json.contains("\"submitter\":\"杜鹏飞\""));
        assertTrue(json.contains("\"sectionLabel\":\"工作周报\""));
    }
}
