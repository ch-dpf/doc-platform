package com.knowbase.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRequestSupportTest {

    @Test
    void buildsDocumentSummaryMessageWithoutRagFraming() {
        ChatRequest request = new ChatRequest(
                "system",
                "Summarize the document content provided below.",
                "Row 1: Alice | Task=Done",
                Map.of("purpose", "document_summary", "temperature", 0.3)
        );
        String message = ChatRequestSupport.buildUserMessage(request);
        assertTrue(message.contains("Document content:"));
        assertTrue(message.contains("Row 1: Alice"));
        assertTrue(!message.contains("请基于以下证据回答问题"));
        assertEquals(0.3, ChatRequestSupport.ollamaOptions(request).get("temperature"));
        assertTrue(!ChatRequestSupport.ollamaOptions(request).containsKey("purpose"));
    }
}
