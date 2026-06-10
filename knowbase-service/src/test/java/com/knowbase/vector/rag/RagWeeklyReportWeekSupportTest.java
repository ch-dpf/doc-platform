package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagWeeklyReportWeekSupportTest {

    @Test
    void extractsYearFromPath() {
        assertEquals(2025, RagWeeklyReportWeekSupport.extractYearFromFileName("2025/杜鹏飞-周报（11.3-11.8）.xlsx"));
        assertEquals(2026, RagWeeklyReportWeekSupport.extractYearFromFileName("2026/杜鹏飞-周报(4.13-4.18).xlsx"));
    }

    @Test
    void formatsWeekLabelFromFilename() {
        assertEquals("11月3日至8日", RagWeeklyReportWeekSupport.formatWeekLabel("2025/杜鹏飞-周报（11.3-11.8）.xlsx"));
        assertEquals("10月9日至17日", RagWeeklyReportWeekSupport.formatWeekLabel("2025/杜鹏飞-周报（10.9-10.17）.xlsx"));
    }

    @Test
    void recognizesWeekQuestion() {
        assertTrue(RagQuestionAnalyzer.isWeeklyReportWeekQuestion(
                "今年哪周的周报有海图项目相关内容？今年是哪一年？"));
        assertTrue(RagQuestionAnalyzer.containsCalendarYearClause(
                "今年哪周的周报有海图项目相关内容？今年是哪一年？"));
    }
}
