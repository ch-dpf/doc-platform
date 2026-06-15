package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagHitRelevanceTest {

    @Test
    void detectsOverlapWhenChunkContainsTerms() {
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "2025年各部门周报须在周五18:00前提交。",
                0.8);
        assertTrue(RagHitRelevance.hasTermOverlap("2025年周报截止时间？", List.of(hit)));
    }

    @Test
    void synthesisQuestionBypassesTermOverlap() {
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "本周完成接口开发",
                0.55);
        assertTrue(RagHitRelevance.hasTermOverlap("有哪些员工上传了周报材料？", List.of(hit)));
    }

    @Test
    void rejectsIrrelevantChunks() {
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "本系统使用 pgvector 存储向量。",
                0.8);
        assertFalse(RagHitRelevance.hasTermOverlap("2025年周报截止时间？", List.of(hit)));
    }
}
