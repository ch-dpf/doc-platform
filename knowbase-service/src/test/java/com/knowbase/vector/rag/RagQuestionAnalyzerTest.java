package com.knowbase.vector.rag;

import com.knowbase.vector.dto.RagChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagQuestionAnalyzerTest {

    @Test
    void synthesisQuestionsRecognized() {
        assertTrue(RagQuestionAnalyzer.isEmployeeCountQuestion("本库中有多少个人提交了周报材料"));
        assertTrue(RagQuestionAnalyzer.isEmployeeCountQuestion("大概有多少员工周报"));
        assertFalse(RagQuestionAnalyzer.isEmployeeListQuestion("本库中有多少个人提交了周报材料"));
        assertTrue(RagQuestionAnalyzer.isWeeklyReportSummaryQuestion("周报主要内容汇总？"));
        assertFalse(RagQuestionAnalyzer.isWeeklyReportSummaryQuestion(
                "有哪些员工提交了周报？都汇报了哪些主要的工作内容？"));
        assertTrue(RagQuestionAnalyzer.isSynthesisQuestion("有哪些员工上传了周报材料？"));
        assertTrue(RagQuestionAnalyzer.isSynthesisQuestion("员工都做了哪些主要的工作内容？"));
        assertTrue(RagQuestionAnalyzer.isEmployeeExistenceQuestion("是否存在其他员工提交周报？"));
        assertTrue(RagQuestionAnalyzer.isCombinedEmployeeWorkQuestion(
                "有哪些员工提交了周报？都汇报了哪些主要的工作内容？"));
        assertFalse(RagQuestionAnalyzer.isCombinedEmployeeWorkQuestion("有哪些员工上传了周报材料？"));
        assertTrue(RagQuestionAnalyzer.isLibraryStatsQuestion("本知识库有多少文档与切片数据？"));
        assertTrue(RagQuestionAnalyzer.isLibraryStatsQuestion("本知识库有多少个文档？"));
        assertTrue(RagQuestionAnalyzer.isLibraryPurposeQuestion("本库主要用于做什么？"));
        assertFalse(RagQuestionAnalyzer.isLibraryPurposeQuestion("本知识库有多少个文档？"));
        assertFalse(RagQuestionAnalyzer.isLibraryPurposeQuestion("本库中员工主要参与了哪些项目？"));
        assertTrue(RagQuestionAnalyzer.isEmployeeProjectCountQuestion("杜鹏飞参与了多少个项目"));
        assertTrue(RagQuestionAnalyzer.isEmployeeProjectQuestion(
                "今年是哪一年？今年杜鹏飞参与了哪些项目？"));
        assertFalse(RagQuestionAnalyzer.isEmployeeProjectQuestion("本库中员工主要参与了哪些项目？"));
        assertTrue(RagQuestionAnalyzer.scopesToCurrentCalendarYear("今年杜鹏飞参与了哪些项目？"));
        assertFalse(RagQuestionAnalyzer.scopesToCurrentCalendarYear("杜鹏飞参与了哪些项目？"));
        assertFalse(RagQuestionAnalyzer.isLibraryStatsQuestion("2025年周报截止时间？"));
        assertTrue(RagQuestionAnalyzer.isSynthesisQuestion("有没有别的员工也提交了周报？"));
        assertFalse(RagQuestionAnalyzer.isSynthesisQuestion("2025年周报截止时间？"));
    }

    @Test
    void calendarYearQuestionRecognized() {
        assertTrue(RagQuestionAnalyzer.isCalendarYearQuestion("今年是哪一年"));
        assertTrue(RagQuestionAnalyzer.isCalendarYearQuestion("今年是哪一年？"));
        assertFalse(RagQuestionAnalyzer.isCalendarYearQuestion("今年参与了哪些项目"));
    }

    @Test
    void projectRecountFollowUpRecognized() {
        assertTrue(RagQuestionAnalyzer.isProjectRecountQuestion(
                "已知上海fb项目、上海浮标项目、FB项目、上海项目为同一个项目，请重新统计"));
        assertFalse(RagQuestionAnalyzer.isProjectRecountQuestion("杜鹏飞参与了多少个项目"));
        var groups = RagQuestionAnalyzer.extractProjectAliasGroups(
                "已知上海fb项目、上海浮标项目、FB项目、上海项目为同一个项目，请重新统计");
        assertEquals(1, groups.size());
        assertEquals(4, groups.get(0).size());
        assertEquals("杜鹏飞", RagQuestionAnalyzer.findNamedEmployeeFromHistory(List.of(
                new RagChatMessage("user", "杜鹏飞参与了多少个项目"),
                new RagChatMessage("assistant", "5 个项目"))));
    }
}
