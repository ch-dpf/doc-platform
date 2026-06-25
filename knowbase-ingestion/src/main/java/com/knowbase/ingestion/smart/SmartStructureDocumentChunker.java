package com.knowbase.ingestion.smart;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.SegmentationConfigResolver;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Production smart chunker: semantic sections with parent context and sentence-window children,
 * aligned with WeKnora-style parent-child retrieval without requiring table_row post-processing.
 */
public final class SmartStructureDocumentChunker {

    private static final Pattern CODE_DECLARATION = Pattern.compile(
            "^(public|private|protected|class|interface|enum|record|def|function|const|let|var|async|static|final|[A-Za-z0-9_<>,\\[\\] ?]+\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\().*"
    );

    public List<DocumentChunk> chunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            LibraryProfile profile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            Map<String, Object> requestOptions
    ) {
        SmartChunkOptions options = SmartChunkOptions.resolve(profile, documentProfile, requestOptions);
        SectionBuffer section = new SectionBuffer();
        List<DocumentChunk> chunks = new ArrayList<>();

        if (document.blocks().isEmpty()) {
            emitFlatFallback(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, options);
            return chunks;
        }

        for (StructuralBlock block : document.blocks()) {
            if ("heading".equals(block.blockType())) {
                flushSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
                section.startHeading(block);
                continue;
            }
            if ("page".equals(block.blockType())) {
                flushSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
                section.startPage(block);
                continue;
            }
            if ("code_block".equals(block.blockType())) {
                flushSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
                emitCode(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, block, options);
                continue;
            }
            if ("table_row".equals(block.blockType())) {
                flushSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
                emitTableRow(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, block);
                continue;
            }
            section.append(block);
        }
        flushSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
        return chunks;
    }

    public static boolean shouldUseSmartEngine(DocumentProfile documentProfile, Map<String, Object> requestOptions) {
        Map<String, Object> merged = SegmentationConfigResolver.mergeOptions(documentProfile, requestOptions);
        String engine = stringOption(merged, "chunkEngine");
        if (engine == null || !"smart".equalsIgnoreCase(engine.trim())) {
            return false;
        }
        String strategy = stringOption(merged, "chunkingStrategy");
        if (strategy == null && documentProfile != null) {
            strategy = documentProfile.chunkingStrategy();
        }
        return strategy == null || !strategy.toLowerCase(Locale.ROOT).contains("table_row");
    }

    private void flushSection(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            LibraryProfile profile,
            List<DocumentChunk> chunks,
            SectionBuffer section,
            SmartChunkOptions options
    ) {
        if (section.isBlank()) {
            section.clear();
            return;
        }
        if (document.contentFamily() == ContentFamily.CODE_OR_CONFIG) {
            emitCode(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section.asCodeBlock(), options);
        } else {
            emitTextSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
        }
        section.clear();
    }

    private void emitTextSection(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            LibraryProfile profile,
            List<DocumentChunk> chunks,
            SectionBuffer section,
            SmartChunkOptions options
    ) {
        String sectionText = section.text();
        if (sectionText.isBlank()) {
            return;
        }
        UUID parentId = UUID.randomUUID();
        String parentText = parentText(section.title(), sectionText, options.parentMaxTokens(), tokenizer);
        Map<String, Object> parentMetadata = sectionMetadata(document, documentProfile, section, "parent");
        parentMetadata.put("chunkTemplate", "parent-child");
        parentMetadata.put("strategy", section.hasPage() ? "pdf-page-section-hybrid" : "semantic-section");
        parentMetadata.put("indexable", false);
        chunks.add(new DocumentChunk(
                parentId,
                documentId,
                libraryId,
                indexVersionId,
                parentText,
                tokenizer.count(parentText).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                profile.embeddingModel(),
                section.boundaryType(),
                null,
                Map.copyOf(parentMetadata)
        ));

        List<String> windows = sentenceWindows(sectionText, options, tokenizer);
        String childStrategy = section.hasPage() ? "pdf-page-section-hybrid" : "semantic+sentence-window";
        for (int index = 0; index < windows.size(); index++) {
            String window = windows.get(index);
            Map<String, Object> childMetadata = sectionMetadata(document, documentProfile, section, "child");
            childMetadata.put("chunkTemplate", "parent-child");
            childMetadata.put("strategy", childStrategy);
            childMetadata.put("windowIndex", index);
            childMetadata.put("windowCount", windows.size());
            childMetadata.put("indexable", true);
            childMetadata.put("chunkEngine", "smart");
            chunks.add(new DocumentChunk(
                    UUID.randomUUID(),
                    documentId,
                    libraryId,
                    indexVersionId,
                    window,
                    tokenizer.count(window).tokens(),
                    tokenizer.tokenizerId(),
                    tokenizer.tokenizerVersion(),
                    profile.embeddingModel(),
                    section.boundaryType(),
                    parentId,
                    Map.copyOf(childMetadata)
            ));
        }
    }

    private void emitCode(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            LibraryProfile profile,
            List<DocumentChunk> chunks,
            StructuralBlock block,
            SmartChunkOptions options
    ) {
        List<String> units = splitCodeByStructure(block.content(), options.maxTokens(), tokenizer);
        for (int index = 0; index < units.size(); index++) {
            Map<String, Object> metadata = baseMetadata(document, documentProfile);
            metadata.put("chunkRole", "flat");
            metadata.put("indexable", true);
            metadata.put("strategy", "code-ast-structural");
            metadata.put("chunkEngine", "smart");
            metadata.put("unitIndex", index);
            metadata.put("unitCount", units.size());
            metadata.put("boundaryType", "code_block");
            String content = units.get(index);
            chunks.add(new DocumentChunk(
                    UUID.randomUUID(),
                    documentId,
                    libraryId,
                    indexVersionId,
                    content,
                    tokenizer.count(content).tokens(),
                    tokenizer.tokenizerId(),
                    tokenizer.tokenizerVersion(),
                    profile.embeddingModel(),
                    "code_block",
                    null,
                    Map.copyOf(metadata)
            ));
        }
    }

    private void emitTableRow(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            LibraryProfile profile,
            List<DocumentChunk> chunks,
            StructuralBlock block
    ) {
        Map<String, Object> metadata = baseMetadata(document, documentProfile);
        metadata.putAll(block.metadata());
        metadata.put("chunkRole", "flat");
        metadata.put("indexable", true);
        metadata.put("strategy", "table-row-inline");
        metadata.put("chunkEngine", "smart");
        String content = block.content() == null ? "" : block.content().trim();
        if (content.isBlank()) {
            return;
        }
        chunks.add(new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                indexVersionId,
                content,
                tokenizer.count(content).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                profile.embeddingModel(),
                "table_row",
                null,
                Map.copyOf(metadata)
        ));
    }

    private void emitFlatFallback(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            LibraryProfile profile,
            List<DocumentChunk> chunks,
            SmartChunkOptions options
    ) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return;
        }
        SectionBuffer section = new SectionBuffer();
        section.appendText(text, Map.of("boundaryType", "paragraph"));
        emitTextSection(libraryId, documentId, indexVersionId, document, documentProfile, tokenizer, profile, chunks, section, options);
    }

    private static Map<String, Object> sectionMetadata(
            ParsedDocument document,
            DocumentProfile documentProfile,
            SectionBuffer section,
            String chunkRole
    ) {
        Map<String, Object> metadata = baseMetadata(document, documentProfile);
        metadata.putAll(section.metadata());
        metadata.put("chunkRole", chunkRole);
        if (!section.title().isBlank()) {
            metadata.put("sectionTitle", section.title());
        }
        return metadata;
    }

    private static Map<String, Object> baseMetadata(ParsedDocument document, DocumentProfile documentProfile) {
        Map<String, Object> metadata = new HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.put("sourceUri", document.sourceUri());
        metadata.put("title", document.title());
        metadata.put("contentFamily", document.contentFamily().name());
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
            metadata.put("parserCode", documentProfile.parserCode());
            metadata.put("chunkingStrategy", documentProfile.chunkingStrategy());
        }
        return metadata;
    }

    private static String parentText(String title, String text, int parentMaxTokens, ModelTokenizer tokenizer) {
        String prefix = title == null || title.isBlank() ? "" : title + "\n";
        String normalized = normalizeText(prefix + text);
        if (tokenizer.count(normalized).tokens() <= parentMaxTokens) {
            return normalized;
        }
        return splitByCharacterBudget(normalized, parentMaxTokens, tokenizer).getFirst();
    }

    private static List<String> sentenceWindows(String text, SmartChunkOptions options, ModelTokenizer tokenizer) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return List.of();
        }
        List<String> windows = new ArrayList<>();
        int index = 0;
        while (index < sentences.size()) {
            int end = Math.min(sentences.size(), index + options.sentenceWindowSize());
            String window = String.join(" ", sentences.subList(index, end));
            if (tokenizer.count(window).tokens() <= options.maxTokens()) {
                windows.add(window);
            } else {
                windows.addAll(splitByCharacterBudget(window, options.maxTokens(), tokenizer));
            }
            if (end == sentences.size()) {
                break;
            }
            index = Math.max(index + 1, end - options.sentenceOverlap());
        }
        return windows;
    }

    private static List<String> splitSentences(String text) {
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < normalized.length(); index++) {
            char ch = normalized.charAt(index);
            current.append(ch);
            if (isSentenceTerminator(ch)) {
                addSentence(sentences, current);
            } else if (ch == '\n' && current.length() > 120) {
                addSentence(sentences, current);
            }
        }
        addSentence(sentences, current);
        return sentences;
    }

    private static void addSentence(List<String> sentences, StringBuilder current) {
        String sentence = normalizeText(current.toString());
        if (!sentence.isBlank()) {
            sentences.add(sentence);
        }
        current.setLength(0);
    }

    private static boolean isSentenceTerminator(char ch) {
        return ch == '.' || ch == '!' || ch == '?' || ch == ';'
                || ch == '。' || ch == '！' || ch == '？' || ch == '；';
    }

    private static List<String> splitByCharacterBudget(String text, int maxTokens, ModelTokenizer tokenizer) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            String candidate = current.toString() + Character.toChars(codePoint);
            if (!current.isEmpty() && tokenizer.count(candidate).tokens() > maxTokens) {
                parts.add(current.toString().trim());
                current.setLength(0);
                current.appendCodePoint(codePoint);
            } else {
                current.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts.stream().filter(part -> !part.isBlank()).toList();
    }

    private static List<String> splitCodeByStructure(String code, int maxTokens, ModelTokenizer tokenizer) {
        String[] lines = code.split("\\R", -1);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            boolean boundary = isCodeBoundary(trimmed);
            if (boundary && !current.isEmpty() && tokenizer.count(current.toString()).tokens() >= maxTokens / 2) {
                chunks.add(current.toString().stripTrailing());
                current.setLength(0);
            }
            current.append(line).append('\n');
            if (tokenizer.count(current.toString()).tokens() > maxTokens) {
                chunks.addAll(splitByCharacterBudget(current.toString(), maxTokens, tokenizer));
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().stripTrailing());
        }
        return chunks.stream().filter(part -> !part.isBlank()).toList();
    }

    private static boolean isCodeBoundary(String line) {
        return !line.isBlank() && (CODE_DECLARATION.matcher(line).matches() || line.startsWith("@"));
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.trim().replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n");
    }

    private static String stringOption(Map<String, Object> options, String key) {
        Object value = options.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private record SmartChunkOptions(int maxTokens, int parentMaxTokens, int sentenceWindowSize, int sentenceOverlap) {

        static SmartChunkOptions resolve(
                LibraryProfile libraryProfile,
                DocumentProfile documentProfile,
                Map<String, Object> requestOptions
        ) {
            Map<String, Object> merged = SegmentationConfigResolver.mergeOptions(documentProfile, requestOptions);
            int maxTokens = readInt(merged, "chunkMaxTokens", libraryProfile == null ? 512 : libraryProfile.chunkMaxTokens());
            int parentMaxTokens = readInt(merged, "smartParentMaxTokens", Math.max(maxTokens * 2, 1200));
            int sentenceWindowSize = readInt(merged, "smartSentenceWindowSize", 4);
            int sentenceOverlap = readInt(merged, "smartSentenceOverlap", 1);
            return new SmartChunkOptions(maxTokens, parentMaxTokens, sentenceWindowSize, sentenceOverlap);
        }

        private static int readInt(Map<String, Object> options, String key, int defaultValue) {
            Object configured = options.get(key);
            if (configured instanceof Number number) {
                return number.intValue();
            }
            if (configured != null) {
                try {
                    return Integer.parseInt(String.valueOf(configured).trim());
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }
    }

    private static final class SectionBuffer {
        private final StringBuilder text = new StringBuilder();
        private final Map<String, Object> metadata = new LinkedHashMap<>();
        private String title = "";

        void startHeading(StructuralBlock block) {
            title = block.content() == null ? "" : block.content().trim();
            metadata.put("boundaryType", "section");
            metadata.put("sectionTitle", title);
            metadata.put("headingLevel", block.level());
        }

        void startPage(StructuralBlock block) {
            title = "Page " + block.metadata().getOrDefault("pageNumber", block.ordinal());
            metadata.putAll(block.metadata());
            metadata.put("boundaryType", "page");
        }

        void append(StructuralBlock block) {
            appendText(block.content(), block.metadata());
        }

        void appendText(String content, Map<String, Object> blockMetadata) {
            if (content == null || content.isBlank()) {
                return;
            }
            if (text.length() > 0) {
                text.append("\n\n");
            }
            text.append(content.trim());
            if (blockMetadata != null) {
                blockMetadata.forEach((key, value) -> {
                    if (value != null && !metadata.containsKey(key)) {
                        metadata.put(key, value);
                    }
                });
            }
            if (!metadata.containsKey("boundaryType") && blockMetadata != null && blockMetadata.containsKey("boundaryType")) {
                metadata.put("boundaryType", blockMetadata.get("boundaryType"));
            }
        }

        StructuralBlock asCodeBlock() {
            return StructuralBlock.codeBlock(text.toString(), 0);
        }

        String text() {
            return text.toString();
        }

        String title() {
            return title;
        }

        Map<String, Object> metadata() {
            return Map.copyOf(metadata);
        }

        String boundaryType() {
            Object boundary = metadata.get("boundaryType");
            return boundary == null ? "paragraph" : String.valueOf(boundary);
        }

        boolean hasPage() {
            return metadata.containsKey("pageNumber");
        }

        boolean isBlank() {
            return text.isEmpty();
        }

        void clear() {
            text.setLength(0);
            metadata.clear();
            title = "";
        }
    }
}
