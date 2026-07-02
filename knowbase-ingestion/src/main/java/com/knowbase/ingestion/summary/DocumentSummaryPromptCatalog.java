package com.knowbase.ingestion.summary;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads document summary prompt templates from classpath YAML (WeKnora-style).
 */
public final class DocumentSummaryPromptCatalog {

    private static final String DEFAULT_RESOURCE = "prompt_templates/generate_summary.yaml";

    private final Map<String, DocumentSummaryPromptTemplate> templatesById;

    public DocumentSummaryPromptCatalog() {
        this(DEFAULT_RESOURCE);
    }

    public DocumentSummaryPromptCatalog(String resourcePath) {
        this.templatesById = load(resourcePath);
    }

    public DocumentSummaryPromptTemplate require(String promptId) {
        String resolvedId = promptId == null || promptId.isBlank() ? defaultId() : promptId.trim();
        DocumentSummaryPromptTemplate template = templatesById.get(resolvedId);
        if (template == null) {
            throw new IllegalArgumentException("Unknown document summary prompt template: " + resolvedId);
        }
        return template;
    }

    public Optional<DocumentSummaryPromptTemplate> find(String promptId) {
        if (promptId == null || promptId.isBlank()) {
            return Optional.ofNullable(templatesById.get(defaultId()));
        }
        return Optional.ofNullable(templatesById.get(promptId.trim()));
    }

    public String defaultId() {
        return templatesById.values().stream()
                .filter(DocumentSummaryPromptTemplate::defaultTemplate)
                .map(DocumentSummaryPromptTemplate::id)
                .findFirst()
                .orElse("default_summary");
    }

    public String render(String promptId, Map<String, String> variables) {
        DocumentSummaryPromptTemplate template = require(promptId);
        String rendered = template.content();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                rendered = rendered.replace(placeholder, entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return rendered.trim();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, DocumentSummaryPromptTemplate> load(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        try (InputStream input = DocumentSummaryPromptCatalog.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing prompt template resource: " + resourcePath);
            }
            Object loaded = new Yaml().load(input);
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new IllegalStateException("Invalid prompt template YAML: " + resourcePath);
            }
            Object templatesNode = root.get("templates");
            if (!(templatesNode instanceof List<?> templates)) {
                throw new IllegalStateException("Prompt template YAML missing templates list: " + resourcePath);
            }
            Map<String, DocumentSummaryPromptTemplate> byId = new LinkedHashMap<>();
            for (Object item : templates) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                String id = stringValue(map.get("id"));
                if (id == null || id.isBlank()) {
                    continue;
                }
                DocumentSummaryPromptTemplate template = new DocumentSummaryPromptTemplate(
                        id,
                        stringValue(map.get("name")),
                        stringValue(map.get("description")),
                        booleanValue(map.get("default")),
                        stringValue(map.get("content"))
                );
                byId.put(id, template);
            }
            if (byId.isEmpty()) {
                throw new IllegalStateException("No prompt templates loaded from: " + resourcePath);
            }
            return Map.copyOf(byId);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load prompt templates: " + resourcePath, exception);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
