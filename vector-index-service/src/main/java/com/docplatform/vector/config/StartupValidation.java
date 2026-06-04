package com.docplatform.vector.config;

import com.docplatform.vector.client.OllamaChatClient;
import com.docplatform.vector.client.OllamaEmbeddingClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupValidation {

    private final OllamaEmbeddingClient embeddingClient;
    private final OllamaChatClient chatClient;
    private final RagProperties ragProperties;

    public StartupValidation(
            OllamaEmbeddingClient embeddingClient,
            OllamaChatClient chatClient,
            RagProperties ragProperties) {
        this.embeddingClient = embeddingClient;
        this.chatClient = chatClient;
        this.ragProperties = ragProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        embeddingClient.validateOnStartup();
        if (ragProperties.isEnabled()) {
            chatClient.validateOnStartup();
        }
    }
}
