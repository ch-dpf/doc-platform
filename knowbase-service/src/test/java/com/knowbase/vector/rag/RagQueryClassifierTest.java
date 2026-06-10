package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagQueryClassifierTest {

    @Test
    void greetingAndModelMetaAreConversational() {
        assertTrue(RagQueryClassifier.isConversational("你好"));
        assertTrue(RagQueryClassifier.isConversational("你好，你是什么模型呢？"));
        assertTrue(RagQueryClassifier.isConversational("你是谁"));
        assertTrue(RagQueryClassifier.isConversational("用的什么大模型"));
    }

    @Test
    void knowledgeQuestionsAreNotConversational() {
        assertFalse(RagQueryClassifier.isConversational("什么是向量检索"));
        assertFalse(RagQueryClassifier.isConversational("2025年周报提交截止时间是什么"));
        assertFalse(RagQueryClassifier.isConversational("你好，请问周报截止时间是哪天"));
    }

    @Test
    void calendarYearQuestionIsConversational() {
        assertTrue(RagQueryClassifier.isConversational("今年是哪一年"));
    }
}
