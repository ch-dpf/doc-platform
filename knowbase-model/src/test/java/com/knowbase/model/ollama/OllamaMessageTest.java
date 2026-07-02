package com.knowbase.model.ollama;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaMessageTest {

    @Test
    void userWithImagesPreservesBase64Payload() {
        OllamaMessage message = OllamaMessage.userWithImages("read this page", List.of("aGVsbG8="));
        assertEquals("user", message.role());
        assertEquals("read this page", message.content());
        assertEquals(List.of("aGVsbG8="), message.images());
    }

    @Test
    void textOnlyMessageHasEmptyImages() {
        OllamaMessage message = new OllamaMessage("user", "hello");
        assertTrue(message.images().isEmpty());
    }
}
