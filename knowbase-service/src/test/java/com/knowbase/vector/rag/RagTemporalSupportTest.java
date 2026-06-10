package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagTemporalSupportTest {

    @Test
    void calendarYearQuestionGetsSystemYearAnswer() {
        var answer = RagTemporalSupport.tryCalendarYearAnswer("今年是哪一年");
        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("年"));
        assertTrue(answer.get().contains("今年"));
    }

    @Test
    void unrelatedQuestionSkipped() {
        assertFalse(RagTemporalSupport.tryCalendarYearAnswer("杜鹏飞参与了多少个项目").isPresent());
    }
}
