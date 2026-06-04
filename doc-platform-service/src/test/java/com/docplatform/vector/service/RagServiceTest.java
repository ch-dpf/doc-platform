package com.docplatform.vector.service;

import com.docplatform.vector.client.OllamaChatClient;
import com.docplatform.vector.config.RagProperties;
import com.docplatform.library.service.VectorLibraryService;
import com.docplatform.vector.dto.RagChatRequest;
import com.docplatform.vector.dto.SearchResponse;
import com.docplatform.vector.rag.RagPromptBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorSearchService searchService;
    @Mock
    private OllamaChatClient chatClient;
    @Mock
    private RagPromptBuilder promptBuilder;

    @Test
    void noHitsReturnsNotFoundWithoutLlm() {
        RagProperties props = new RagProperties();
        RagService service = new RagService(props, searchService, chatClient, promptBuilder);
        when(searchService.search(any())).thenReturn(new SearchResponse(List.of()));

        var response = service.chat(
                new RagChatRequest(
                        VectorLibraryService.DEFAULT_LIBRARY_ID, "demo", "未知问题", 5, null, null, null));

        assertFalse(response.found());
        assertFalse(response.usedLlm());
        assertTrue(response.answer().startsWith("未找到"));
        verify(chatClient, never()).chat(anyString(), anyString());
    }
}
