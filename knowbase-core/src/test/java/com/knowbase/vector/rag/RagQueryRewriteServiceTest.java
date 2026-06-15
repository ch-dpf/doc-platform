package com.knowbase.vector.rag;

import com.knowbase.vector.client.OllamaChatClient;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.config.RetrievalProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagQueryRewriteServiceTest {

    @Mock
    private OllamaChatClient chatClient;

    private RetrievalProperties retrievalProperties;
    private RagQueryRewriteService service;

    @BeforeEach
    void setUp() {
        retrievalProperties = new RetrievalProperties();
        retrievalProperties.setQueryRewriteEnabled(true);
        retrievalProperties.setQueryRewriteMaxChars(96);
        OllamaProperties ollama = new OllamaProperties();
        service = new RagQueryRewriteService(chatClient, ollama, retrievalProperties);
    }

    @Test
    void expandsProjectParticipationSynthesisWithoutLlm() {
        String rewritten = service.rewrite(
                "本库中员工主要参与了哪些项目？",
                "本库中员工主要参与了哪些项目？",
                List.of());

        assertEquals("参与人 项目 员工 主要", rewritten);
        verify(chatClient, never()).chat(anyString(), any(), anyString(), anyString());
    }

    @Test
    void rewritesWeeklySynthesisWhenRuleExpansionWeak() {
        when(chatClient.chat(anyString(), any(), anyString(), anyString()))
                .thenReturn("周报 工作内容 主要事项 员工 汇总");

        String rewritten = service.rewrite("周报主要内容汇总？", "周报主要内容汇总？", List.of());

        assertEquals("周报 工作内容 主要事项 员工 汇总", rewritten);
    }

    @Test
    void rejectsLibraryMetadataStyleRewrite() {
        assertTrue(RagQueryRewriteService.looksLikeLibraryMetadataRewrite(
                "知识库「项目参与人库」：项目名称、参与人、部门、工作内容、截止时间"));
    }

    @Test
    void skipsRewriteWhenDisabled() {
        retrievalProperties.setQueryRewriteEnabled(false);

        String result = service.rewrite("周报主要内容汇总？", "周报主要内容汇总？", List.of());

        assertEquals("周报主要内容汇总？", result);
        verify(chatClient, never()).chat(anyString(), any(), anyString(), anyString());
    }

    @Test
    void skipsRewriteForKeywordRichShortQuery() {
        assertTrue(RagQueryRewriteService.shouldSkipRewrite("2025 周报 截止 时间", "2025周报截止时间？"));

        String result = service.rewrite("2025 周报 截止 时间", "2025周报截止时间？", List.of());

        assertEquals("2025 周报 截止 时间", result);
        verify(chatClient, never()).chat(anyString(), any(), anyString(), anyString());
    }

    @Test
    void sanitizeStripsMarkdownAndLabels() {
        assertEquals(
                "周报 工作内容 汇总",
                RagQueryRewriteService.sanitizeRewrite("```\n改写：周报 工作内容 汇总？\n```"));
    }

    @Test
    void synthesisQuestionNeverSkipped() {
        assertFalse(RagQueryRewriteService.shouldSkipRewrite("周报主要内容汇总？", "周报主要内容汇总？"));
    }
}
