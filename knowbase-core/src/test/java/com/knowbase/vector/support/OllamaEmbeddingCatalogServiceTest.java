package com.knowbase.vector.support;

import com.knowbase.library.dto.EmbeddingCatalogResponse;
import com.knowbase.library.dto.EmbeddingModelDescriptor;
import com.knowbase.vector.client.OllamaEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingCatalogServiceTest {

    @Mock
    private OllamaEmbeddingClient embeddingClient;

    @Test
    void normalizeModelIdStripsTagSuffix() {
        assertEquals("nomic-embed-text", OllamaEmbeddingCatalogService.normalizeModelId("nomic-embed-text:latest"));
        assertEquals("llama3.2", OllamaEmbeddingCatalogService.normalizeModelId("llama3.2"));
    }

    @Test
    void catalogSkipsLikelyChatModels() {
        OllamaEmbeddingCatalogService spyService = new OllamaEmbeddingCatalogService(null, embeddingClient) {
            @Override
            List<String> listLocalModelIds() {
                return List.of("nomic-embed-text", "llama3.2", "bge-m3");
            }
        };

        when(embeddingClient.embed(anyString(), eq("nomic-embed-text"), eq(0))).thenReturn(new float[768]);
        when(embeddingClient.embed(anyString(), eq("bge-m3"), eq(0))).thenReturn(new float[1024]);

        EmbeddingCatalogResponse catalog = spyService.catalog();

        assertEquals("ollama", catalog.provider());
        assertEquals("pgvector", catalog.vectorStoreType());
        assertEquals(2, catalog.models().size());
        assertTrue(catalog.models().stream().anyMatch(m -> "nomic-embed-text".equals(m.modelId())));
        assertTrue(catalog.models().stream().anyMatch(m -> "bge-m3".equals(m.modelId())));
        verify(embeddingClient, never()).embed(anyString(), eq("llama3.2"), anyInt());
    }

    @Test
    void catalogIncludesDetectedDimensions() {
        OllamaEmbeddingCatalogService spyService = new OllamaEmbeddingCatalogService(null, embeddingClient) {
            @Override
            List<String> listLocalModelIds() {
                return List.of("nomic-embed-text");
            }
        };
        when(embeddingClient.embed(anyString(), eq("nomic-embed-text"), eq(0))).thenReturn(new float[768]);

        EmbeddingModelDescriptor model = spyService.catalog().models().get(0);

        assertEquals("nomic-embed-text", model.modelId());
        assertEquals(768, model.dimension());
    }
}
