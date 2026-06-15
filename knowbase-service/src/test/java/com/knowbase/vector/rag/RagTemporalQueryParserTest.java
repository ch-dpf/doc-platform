package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagTemporalQueryParserTest {

    @Test
    void parsesYearMonthAndPerson() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞2025年9月都完成了哪些主要工作内容？", null);

        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
        assertEquals("杜鹏飞", scope.person());
        assertTrue(scope.completedWorkOnly());
        assertEquals(TemporalParseConfidence.HIGH, scope.confidence());
    }

    @Test
    void parsesCurrentYearMonth() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse("今年9月完成了哪些工作？", null);

        assertTrue(scope.scoped());
        assertEquals(RagTemporalSupport.currentCalendarYear(), scope.year());
        assertEquals(9, scope.month());
    }

    @Test
    void parsesYearMonthAndPersonWithZai() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞在2025年9月完成了哪些主要工作内容？", null);

        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
        assertEquals("杜鹏飞", scope.person());
        assertTrue(scope.completedWorkOnly());
    }

    @Test
    void parsesNameContainingZaiCharacter() {
        TemporalQueryScope withoutConnector = RagTemporalQueryParser.parse(
                "文在寅2025年9月完成了哪些主要工作内容？", null);
        assertEquals("文在寅", withoutConnector.person());

        TemporalQueryScope withConnector = RagTemporalQueryParser.parse(
                "文在寅在2025年9月完成了哪些主要工作内容？", null);
        assertEquals("文在寅", withConnector.person());
    }

    @Test
    void parsesMonthRange() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse("2025年9-10月完成了哪些工作？", null);
        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
        assertEquals(10, scope.monthEnd());
        assertEquals(List.of(9, 10), scope.monthsInRange());
    }

    @Test
    void parsesQuarter() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse("2025年第三季度工作内容", null);
        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(7, scope.month());
        assertEquals(9, scope.monthEnd());
    }

    @Test
    void parsesMultiPerson() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse("杜鹏飞和李明2025年9月完成了哪些工作？", null);
        assertTrue(scope.scoped());
        assertEquals(List.of("杜鹏飞", "李明"), scope.persons());
    }

    @Test
    void returnsNoneWhenNoMonth() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse("杜鹏飞参与了哪些项目？", null);
        assertFalse(scope.scoped());
    }

    @Test
    void parsesMonthWeek() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞在2025年9月第一周完成了哪些工作？", null);

        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
        assertEquals(1, scope.weekOfMonth());
        assertEquals("杜鹏飞", scope.person());
        assertEquals(LocalDate.of(2025, 9, 1), scope.rangeStart().orElseThrow());
        assertEquals(LocalDate.of(2025, 9, 7), scope.rangeEnd().orElseThrow());
        assertTrue(scope.toSummary().contains("第1周"));
    }

    @Test
    void parsesMonthDay() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞2025年9月第一天都完成了哪些工作", null);

        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
        assertEquals(1, scope.dayOfMonth());
        assertEquals("杜鹏飞", scope.person());
        assertEquals(java.time.LocalDate.of(2025, 9, 1), scope.rangeStart().orElseThrow());
        assertEquals(java.time.LocalDate.of(2025, 9, 1), scope.rangeEnd().orElseThrow());
        assertTrue(scope.toSummary().contains("9月1日"));
    }

    @Test
    void parsesOrdinalMonth() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞2025年第九个月都完成了哪些工作？", null);

        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
        assertEquals("杜鹏飞", scope.person());
        assertTrue(scope.completedWorkOnly());
    }

    @Test
    void parsesChineseMonthName() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞在2025年九月完成了哪些工作？", null);

        assertTrue(scope.scoped());
        assertEquals(2025, scope.year());
        assertEquals(9, scope.month());
    }

    @Test
    void parsesYearOnly() {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(
                "杜鹏飞2025年都完成了哪些工作？", null);

        assertTrue(scope.scoped());
        assertTrue(scope.yearScoped());
        assertEquals(2025, scope.year());
        assertEquals(null, scope.month());
        assertEquals("杜鹏飞", scope.person());
        assertTrue(scope.completedWorkOnly());
        assertEquals(LocalDate.of(2025, 1, 1), scope.rangeStart().orElseThrow());
        assertEquals(LocalDate.of(2025, 12, 31), scope.rangeEnd().orElseThrow());
        assertEquals(12, scope.monthsInRange().size());
        assertTrue(scope.toSummary().contains("2025年"));
    }

    @Test
    void yearOnlyDoesNotMatchWeeklyReportDeadlineQuestion() {
        assertFalse(RagTemporalTimeParser.parse("2025年周报截止时间？").scoped());
        assertTrue(RagTemporalQueryParser.parse("杜鹏飞2025年都完成了哪些工作？", null).yearScoped());
    }

    @Test
    void scopeSummaryIncludesConfidence() {
        TemporalQueryScope scope = TemporalQueryScope.scopedSingleMonth(
                2025, 9, "杜鹏飞", true, TemporalParseConfidence.HIGH);
        assertTrue(scope.toSummary().contains("杜鹏飞"));
        assertTrue(scope.toSummary().contains("HIGH"));
    }
}
