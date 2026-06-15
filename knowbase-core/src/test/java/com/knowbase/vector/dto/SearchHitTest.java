package com.knowbase.vector.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchHitTest {

    @Test
    void contextForPromptIncludesParentWhenPresent() {
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "子块片段",
                0.9,
                "父章节完整上下文");
        String prompt = hit.contextForPrompt();
        assertTrue(prompt.contains("【章节上下文】"));
        assertTrue(prompt.contains("父章节完整上下文"));
        assertTrue(prompt.contains("【命中片段】"));
        assertTrue(prompt.contains("子块片段"));
    }
}
