package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEmployeeRosterSupportTest {

    @Test
    void formatsExistenceAnswerForSingleSubmitter() {
        var names = new java.util.LinkedHashSet<String>();
        names.add("杜鹏飞");
        String answer = RagEmployeeRosterSupport.formatExistenceAnswer(names);
        assertTrue(answer.contains("不存在其他员工"));
        assertTrue(answer.contains("杜鹏飞"));
    }

    @Test
    void formatsListAnswer() {
        var names = new java.util.LinkedHashSet<String>();
        names.add("杜鹏飞");
        String answer = RagEmployeeRosterSupport.formatListAnswer(names);
        assertTrue(answer.contains("杜鹏飞"));
        assertTrue(answer.contains("上传周报材料的员工"));
    }

    @Test
    void formatsCountAnswerForSingleSubmitter() {
        var names = new java.util.LinkedHashSet<String>();
        names.add("杜鹏飞");
        String answer = RagEmployeeRosterSupport.formatCountAnswer(names);
        assertTrue(answer.contains("共有 1 人"));
        assertTrue(answer.contains("杜鹏飞"));
        assertFalse(answer.contains("总计"));
    }

    @Test
    void formatsCountAnswerForMultipleSubmitters() {
        var names = new java.util.LinkedHashSet<java.lang.String>();
        names.add("杜鹏飞");
        names.add("王小明");
        String answer = RagEmployeeRosterSupport.formatCountAnswer(names);
        assertTrue(answer.contains("共有 2 人"));
        assertTrue(answer.contains("杜鹏飞"));
        assertTrue(answer.contains("王小明"));
    }
}
