package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagProjectParticipationSupportTest {

    private static final String ROWS = """
            1\t海图项目\t配合苏研院同事编写用户手册\t\t45896\t杜鹏飞\t配合完成用户手册编写\t\t已完成

            2\tFB项目\t按照0336协议调整fb回传数据\t\t46127\t杜鹏飞\t\t\t已完成

            3\t上海浮标项目\t研究调用动态链接库接口\t\t45982\t杜鹏飞\t完成相关任务\t\t待开展
            """;

    @Test
    void extractsTargetPerson() {
        assertEquals("杜鹏飞", RagProjectParticipationSupport.extractTargetPerson("杜鹏飞参与了多少个项目"));
        assertEquals("杜鹏飞", RagProjectParticipationSupport.extractTargetPerson("杜鹏飞参与了多少个项目？"));
        assertEquals("杜鹏飞", RagProjectParticipationSupport.extractTargetPerson(
                "今年是哪一年？今年杜鹏飞参与了哪些项目？"));
    }

    @Test
    void compoundProjectQuestionRecognizedAndYearScoped() {
        String question = "今年是哪一年？今年杜鹏飞参与了哪些项目？";
        assertTrue(RagQuestionAnalyzer.isEmployeeProjectQuestion(question));
        assertTrue(RagQuestionAnalyzer.scopesToCurrentCalendarYear(question));
        assertTrue(RagQuestionAnalyzer.containsCalendarYearClause(question));
    }

    @Test
    void doesNotTreatLibraryWideProjectQuestionAsNamedEmployeeProject() {
        assertEquals(null, RagProjectParticipationSupport.extractTargetPerson("本库中员工主要参与了哪些项目？"));
        assertFalse(RagQuestionAnalyzer.isEmployeeProjectQuestion("本库中员工主要参与了哪些项目？"));
    }

    @Test
    void extractsDistinctProjectsFromChunk() {
        var projects = RagProjectParticipationSupport.extractDistinctProjects(ROWS);
        assertEquals(3, projects.size());
        assertTrue(projects.contains("海图项目"));
        assertTrue(projects.contains("FB项目"));
        assertTrue(projects.contains("上海浮标项目"));
    }
}
