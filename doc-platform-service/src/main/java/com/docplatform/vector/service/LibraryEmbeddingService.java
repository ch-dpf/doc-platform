package com.docplatform.vector.service;

import com.docplatform.library.config.EmbeddingSpec;
import com.docplatform.library.service.LibraryConfigResolver;
import com.docplatform.library.service.UnsupportedEmbeddingProviderException;
import com.docplatform.vector.client.OllamaEmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 按向量库配置路由 Embedding 模型（一期仅 Ollama）。
 */
@Service
public class LibraryEmbeddingService {

    private final LibraryConfigResolver libraryConfigResolver;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    public LibraryEmbeddingService(
            LibraryConfigResolver libraryConfigResolver, OllamaEmbeddingClient ollamaEmbeddingClient) {
        this.libraryConfigResolver = libraryConfigResolver;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
    }

    public float[] embed(UUID libraryId, String text) {
        EmbeddingSpec spec = libraryConfigResolver.embeddingFor(libraryId);
        requireOllama(spec);
        return ollamaEmbeddingClient.embed(text, spec.model(), spec.dimension());
    }

    public List<float[]> embedBatch(UUID libraryId, List<String> texts) {
        EmbeddingSpec spec = libraryConfigResolver.embeddingFor(libraryId);
        requireOllama(spec);
        return ollamaEmbeddingClient.embedBatch(texts, spec.model(), spec.dimension());
    }

    private static void requireOllama(EmbeddingSpec spec) {
        if (!"ollama".equalsIgnoreCase(spec.provider())) {
            throw new UnsupportedEmbeddingProviderException(spec.provider());
        }
    }
}
