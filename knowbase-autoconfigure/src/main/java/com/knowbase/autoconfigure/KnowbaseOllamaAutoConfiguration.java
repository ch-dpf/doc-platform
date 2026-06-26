package com.knowbase.autoconfigure;

import com.knowbase.model.ChatModelClient;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.model.ollama.OllamaChatModelClient;
import com.knowbase.model.ollama.OllamaClient;
import com.knowbase.model.ollama.OllamaEmbeddingModelClient;
import com.knowbase.tokenizer.DefaultTokenizerRegistry;
import com.knowbase.tokenizer.OllamaTokenizerRegistry;
import com.knowbase.tokenizer.TokenizerRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "knowbase.ollama", name = "enabled", havingValue = "true")
public class KnowbaseOllamaAutoConfiguration {

    @Bean("knowbaseOllamaClient")
    @ConditionalOnMissingBean(name = "knowbaseOllamaClient")
    OllamaClient knowbaseOllamaClient(KnowbaseProperties properties) {
        KnowbaseProperties.Ollama ollama = properties.getOllama();
        return new OllamaClient(ollama.getBaseUrl(), ollama.getTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelClient.class)
    EmbeddingModelClient ollamaEmbeddingModelClient(OllamaClient ollamaClient, KnowbaseProperties properties) {
        KnowbaseProperties.Ollama ollama = properties.getOllama();
        return new OllamaEmbeddingModelClient(
                ollamaClient,
                ollama.getProvider(),
                ollama.getEmbeddingModel(),
                ollama.getEmbeddingDimension()
        );
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelClient.class)
    ChatModelClient ollamaChatModelClient(OllamaClient ollamaClient, KnowbaseProperties properties) {
        KnowbaseProperties.Ollama ollama = properties.getOllama();
        return new OllamaChatModelClient(ollamaClient, ollama.getProvider(), ollama.getChatModel());
    }

    @Bean
    @ConditionalOnMissingBean(TokenizerRegistry.class)
    TokenizerRegistry ollamaTokenizerRegistry(OllamaClient ollamaClient, KnowbaseProperties properties) {
        return new OllamaTokenizerRegistry(
                ollamaClient,
                properties.getOllama().getProvider(),
                new DefaultTokenizerRegistry()
        );
    }
}
