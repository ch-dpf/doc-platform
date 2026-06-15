package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.rag.TemporalParseConfidence;
import com.knowbase.vector.rag.TemporalQueryScope;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalHitMatcherTest {

    @Test
    void matchesMetadataPeriod() {
        TemporalQueryScope scope = TemporalQueryScope.scopedSingleMonth(
                2025, 9, "杜鹏飞", true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·工作周报】\n2025年9月1日-9月7日\n项目\t已完成",
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-07",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        assertTrue(TemporalHitMatcher.matches(hit, scope, "杜鹏飞周报(9.1-9.7).xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void rejectsPlanOnlySection() {
        TemporalQueryScope scope = TemporalQueryScope.scopedSingleMonth(
                2025, 9, "杜鹏飞", true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·周工作计划】\n项目\t待开展",
                0.9);
        assertFalse(TemporalHitMatcher.matches(hit, scope, "杜鹏飞周报.xlsx", Map.of("sectionLabel", "周工作计划")));
    }

    @Test
    void rejectsWrongPerson() {
        TemporalQueryScope scope = TemporalQueryScope.scopedSingleMonth(
                2025, 9, "杜鹏飞", true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【王明·工作周报】\n项目\t已完成",
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-07",
                "9",
                "王明",
                "工作周报",
                "true");
        assertFalse(TemporalHitMatcher.matches(hit, scope, "王明周报.xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void matchesWithoutCompletedWorkFilterWhenNotRequired() {
        TemporalQueryScope scope = TemporalQueryScope.scopedSingleMonth(
                2025, 9, "杜鹏飞", false, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·周工作计划】\n项目\t待开展",
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-07",
                "9",
                "杜鹏飞",
                "周工作计划",
                "false");
        assertTrue(TemporalHitMatcher.matches(hit, scope, "杜鹏飞周报(9.1-9.7).xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void matchesMonthRangeViaPeriodMonths() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, 9, 10, null, null, java.util.List.of("杜鹏飞"), false, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "content",
                0.9,
                null,
                null,
                "2025",
                null,
                null,
                "9,10",
                "杜鹏飞",
                "工作周报",
                "true");
        assertTrue(TemporalHitMatcher.matches(hit, scope, "杜鹏飞周报.xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void rejectsChunkOutsideWeekScope() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, 9, null, 1, null, java.util.List.of("杜鹏飞"), true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·工作周报·2025年9月15日--9月19日】",
                0.9,
                null,
                null,
                "2025",
                "2025-09-15",
                "2025-09-19",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        assertFalse(TemporalHitMatcher.matches(
                hit, scope, "2025/杜鹏飞-周报（9.15-9.19）.xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void matchesChunkInsideWeekScope() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, 9, null, 1, null, java.util.List.of("杜鹏飞"), true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·工作周报·2025年9月1日--9月6日】",
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        assertTrue(TemporalHitMatcher.matches(
                hit, scope, "2025/杜鹏飞-周报（9.1-9.6）.xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void rejectsCompletedWorkOnWrongDay() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, 9, null, null, 1, java.util.List.of("杜鹏飞"), true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                1\t海图项目\t完成项目部署文档编写\t\t2025.9.4\t杜鹏飞\t完成文档编写\t\t已完成
                """,
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        assertFalse(TemporalHitMatcher.matches(
                hit, scope, "2025/杜鹏飞-周报（9.1-9.6）.xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void matchesCompletedWorkOnExactDay() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, 9, null, null, 1, java.util.List.of("杜鹏飞"), true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                1\t海图项目\t完成首日任务\t\t2025.9.1\t杜鹏飞\t完成\t\t已完成
                """,
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        assertTrue(TemporalHitMatcher.matches(
                hit, scope, "2025/杜鹏飞-周报（9.1-9.6）.xlsx", hit.temporalMetadataMap()));
    }

    @Test
    void matchesYearScopeViaFileName() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, null, null, null, null, java.util.List.of("杜鹏飞"), true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·工作周报·2025年3月1日--3月7日】\n项目\t已完成",
                0.9);
        assertTrue(TemporalHitMatcher.matches(
                hit, scope, "2025/杜鹏飞-周报（3.1-3.7）.xlsx", Map.of()));
    }

    @Test
    void rejectsYearScopeForWrongYear() {
        TemporalQueryScope scope = TemporalQueryScope.scoped(
                2025, null, null, null, null, java.util.List.of("杜鹏飞"), true, TemporalParseConfidence.HIGH);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【杜鹏飞·工作周报·2024年3月1日--3月7日】\n项目\t已完成",
                0.9);
        assertFalse(TemporalHitMatcher.matches(
                hit, scope, "2024/杜鹏飞-周报（3.1-3.7）.xlsx", Map.of()));
    }
}
