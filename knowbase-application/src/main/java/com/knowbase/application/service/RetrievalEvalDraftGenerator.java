package com.knowbase.application.service;

import com.knowbase.api.command.CreateRetrievalEvalSampleCommand;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.KnowledgeDocument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RetrievalEvalDraftGenerator {

    static final int MIN_CHUNK_TOKENS = 30;
    static final int MIN_GROUND_TRUTH_CHARS = 24;
    static final int MAX_DRAFTS_PER_DOCUMENT = 3;
    static final int GROUND_TRUTH_EXCERPT_MAX = 180;

    enum DocumentKind {
        MANUAL,
        FORM,
        SHORT,
        GENERAL
    }

    public List<CreateRetrievalEvalSampleCommand> generate(
            KnowledgeDocument document,
            List<DocumentChunk> chunks,
            int defaultHitRank
    ) {
        return generate(document, chunks, defaultHitRank, MAX_DRAFTS_PER_DOCUMENT);
    }

    public List<CreateRetrievalEvalSampleCommand> generate(
            KnowledgeDocument document,
            List<DocumentChunk> chunks,
            int defaultHitRank,
            int maxDrafts
    ) {
        if (document == null || chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<DocumentChunk> eligible = chunks.stream()
                .filter(chunk -> chunk.content() != null && !chunk.content().isBlank())
                .filter(chunk -> chunk.tokenCount() >= MIN_CHUNK_TOKENS)
                .filter(chunk -> chunk.content().trim().length() >= MIN_GROUND_TRUTH_CHARS)
                .filter(chunk -> !isTitleOnlyChunk(chunk))
                .sorted(Comparator.comparingInt(DocumentChunk::tokenCount).reversed())
                .toList();
        if (eligible.isEmpty()) {
            eligible = chunks.stream()
                    .filter(chunk -> chunk.content() != null && !chunk.content().isBlank())
                    .filter(chunk -> chunk.content().trim().length() >= MIN_GROUND_TRUTH_CHARS)
                    .sorted(Comparator.comparingInt(DocumentChunk::tokenCount).reversed())
                    .limit(1)
                    .toList();
        }
        if (eligible.isEmpty()) {
            return List.of();
        }

        DocumentKind kind = classify(document);
        int perDocumentLimit = kind == DocumentKind.SHORT && eligible.size() <= 1
                ? 1
                : Math.min(Math.max(1, maxDrafts), eligible.size());
        String title = document.title() == null ? "未命名文档" : document.title();
        Set<String> seenQuestions = new LinkedHashSet<>();
        List<CreateRetrievalEvalSampleCommand> drafts = new ArrayList<>();
        for (int index = 0; index < eligible.size() && drafts.size() < perDocumentLimit; index++) {
            DocumentChunk chunk = eligible.get(index);
            String section = sectionTitle(chunk.metadata());
            String question = questionFor(kind, title, section, index);
            if (!seenQuestions.add(normalizeQuestion(question))) {
                continue;
            }
            String groundTruth = excerpt(chunk.content());
            if (groundTruth.length() < 8) {
                continue;
            }
            drafts.add(new CreateRetrievalEvalSampleCommand(
                    question,
                    List.of(document.documentId()),
                    List.of(),
                    List.of(groundTruth),
                    Math.max(1, defaultHitRank),
                    RetrievalEvalDraftSupport.formatNotes("来源分块 token=" + chunk.tokenCount()),
                    false
            ));
        }
        return List.copyOf(drafts);
    }

    private static DocumentKind classify(KnowledgeDocument document) {
        String title = document.title() == null ? "" : document.title();
        if (title.contains("用户使用手册") || title.contains("使用手册")) {
            return DocumentKind.MANUAL;
        }
        if (title.contains("信息表") || title.contains("标准") || title.contains("软著")) {
            return DocumentKind.FORM;
        }
        if (title.contains("开发目的") || title.contains("技术特点")) {
            return DocumentKind.SHORT;
        }
        return DocumentKind.GENERAL;
    }

    private static String questionFor(DocumentKind kind, String title, String section, int index) {
        String docLabel = stripExtension(title);
        String sectionPart = section == null ? "" : section.trim();
        return switch (kind) {
            case MANUAL -> sectionPart.isBlank()
                    ? "《" + docLabel + "》的主要功能与使用说明是什么？"
                    : "根据《" + docLabel + "》，「" + sectionPart + "」部分说明了哪些内容？";
            case FORM -> index == 0
                    ? "《" + docLabel + "》中列出了哪些标准或字段要求？"
                    : "《" + docLabel + "》中关于「" + sectionPart + "」有哪些要求？";
            case SHORT -> {
                if (title.contains("开发目的")) {
                    yield "《" + docLabel + "》所描述的开发目的是什么？";
                }
                if (title.contains("技术特点")) {
                    yield "《" + docLabel + "》中列举了哪些技术特点？";
                }
                yield "《" + docLabel + "》的核心内容是什么？";
            }
            case GENERAL -> sectionPart.isBlank()
                    ? "《" + docLabel + "》包含哪些关键信息？"
                    : "《" + docLabel + "》中关于「" + sectionPart + "」的内容是什么？";
        };
    }

    static boolean isTitleOnlyChunk(DocumentChunk chunk) {
        String content = chunk.content() == null ? "" : chunk.content().trim();
        if (chunk.tokenCount() >= MIN_CHUNK_TOKENS && content.length() >= 60) {
            return false;
        }
        String section = sectionTitle(chunk.metadata());
        if (!section.isBlank() && content.equals(section)) {
            return true;
        }
        return chunk.tokenCount() < 12 && content.length() < 40;
    }

    private static String sectionTitle(Map<String, Object> metadata) {
        if (metadata == null || metadata.get("sectionTitle") == null) {
            return "";
        }
        return String.valueOf(metadata.get("sectionTitle")).trim();
    }

    static String excerpt(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= GROUND_TRUTH_EXCERPT_MAX) {
            return normalized;
        }
        int start = Math.min(Math.max(normalized.length() / 5, 0), 120);
        int end = Math.min(start + GROUND_TRUTH_EXCERPT_MAX, normalized.length());
        return normalized.substring(start, end).trim();
    }

    private static String stripExtension(String title) {
        int dot = title.lastIndexOf('.');
        if (dot > 0) {
            return title.substring(0, dot);
        }
        return title;
    }

    private static String normalizeQuestion(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }
}
