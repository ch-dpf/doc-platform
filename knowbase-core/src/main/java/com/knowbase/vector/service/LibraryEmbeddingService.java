package com.knowbase.vector.service;

import com.knowbase.library.config.EmbeddingSpec;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.library.service.UnsupportedEmbeddingProviderException;
import com.knowbase.vector.client.OllamaEmbeddingClient;
import com.knowbase.vector.config.EmbeddingProperties;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.embedding.EmbeddingInputFormatter;
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
    private final OllamaProperties ollamaProperties;
    private final EmbeddingProperties embeddingProperties;

    public LibraryEmbeddingService(
            LibraryConfigResolver libraryConfigResolver,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            OllamaProperties ollamaProperties,
            EmbeddingProperties embeddingProperties) {
        this.libraryConfigResolver = libraryConfigResolver;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.ollamaProperties = ollamaProperties;
        this.embeddingProperties = embeddingProperties;
    }

    /** 检索 query 向量化（nomic 模型自动加 search_query 前缀）。 */
    public float[] embed(UUID libraryId, String text) {
        EmbeddingSpec spec = libraryConfigResolver.embeddingFor(libraryId);
        requireOllama(spec);
        String input = EmbeddingInputFormatter.forSearchQuery(text, spec.model());
        return ollamaEmbeddingClient.embed(input, spec.model(), spec.dimension());
    }

    /** 文档 chunk 向量化（nomic 模型自动加 search_document 前缀）。 */
    public List<float[]> embedBatch(UUID libraryId, List<String> texts) {
        EmbeddingSpec spec = libraryConfigResolver.embeddingFor(libraryId);
        requireOllama(spec);
        List<String> inputs = EmbeddingInputFormatter.forSearchDocuments(texts, spec.model());
        return ollamaEmbeddingClient.embedBatch(inputs, spec.model(), spec.dimension());
    }

    /** 重排等场景的 query 向量化（按实际模型决定是否加 search_query 前缀）。 */
    public float[] embedWithModel(UUID libraryId, String text, String modelOverride) {
        EmbeddingSpec spec = libraryConfigResolver.embeddingFor(libraryId);
        requireOllama(spec);
        String model = resolveModel(modelOverride, spec.model());
        String input = EmbeddingInputFormatter.forSearchQuery(text, model);
        return ollamaEmbeddingClient.embed(input, model, spec.dimension());
    }

    /** 重排等场景的文档向量化（按实际模型决定是否加 search_document 前缀）。 */
    public List<float[]> embedBatchWithModel(UUID libraryId, List<String> texts, String modelOverride) {
        EmbeddingSpec spec = libraryConfigResolver.embeddingFor(libraryId);
        requireOllama(spec);
        String model = resolveModel(modelOverride, spec.model());
        List<String> inputs = EmbeddingInputFormatter.forSearchDocuments(texts, model);
        return ollamaEmbeddingClient.embedBatch(inputs, model, spec.dimension());
    }

    public List<float[]> embedBatchWithDefaultModel(List<String> texts) {
        EmbeddingSpec spec = defaultEmbeddingSpec();
        requireOllama(spec);
        List<String> inputs = EmbeddingInputFormatter.forSearchDocuments(texts, spec.model());
        return ollamaEmbeddingClient.embedBatch(inputs, spec.model(), spec.dimension());
    }

    private EmbeddingSpec defaultEmbeddingSpec() {
        return new EmbeddingSpec("ollama", ollamaProperties.getEmbeddingModel(), embeddingProperties.getDimension());
    }

    private static void requireOllama(EmbeddingSpec spec) {
        if (!"ollama".equalsIgnoreCase(spec.provider())) {
            throw new UnsupportedEmbeddingProviderException(spec.provider());
        }
    }

    private static String resolveModel(String modelOverride, String libraryModel) {
        if (modelOverride != null && !modelOverride.isBlank()) {
            return modelOverride.trim();
        }
        return libraryModel;
    }
}
