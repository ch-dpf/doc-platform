package com.knowbase.ingestion.chunking;

import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.BlockType;
import com.knowbase.ingestion.document.ParsedDocument.CodeBlock;
import com.knowbase.ingestion.document.ParsedDocument.ContentFamily;
import com.knowbase.ingestion.document.ParsedDocument.DocumentBlock;
import com.knowbase.ingestion.document.ParsedDocument.FaqBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableCell;
import com.knowbase.ingestion.document.ParsedDocument.TextBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Default chunker for mixed-family RAG documents.
 *
 * <p>Priority order: semantic structure boundaries, token budget, character fallback.</p>
 */
public final class SmartDocumentChunker implements DocumentChunker {

    private static final Pattern CODE_DECLARATION = Pattern.compile(
            "^(public|private|protected|class|interface|enum|record|def|function|const|let|var|async|static|final|[A-Za-z0-9_<>,\\[\\] ?]+\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\().*"
    );

    @Override
    public List<DocumentChunk> chunk(ParsedDocument document) {
        return chunk(document, ChunkingOptions.defaults());
    }

    @Override
    public List<DocumentChunk> chunk(ParsedDocument document, ChunkingOptions options) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(options, "options");
        ChunkSink sink = new ChunkSink(document.documentId());
        SectionBuffer section = new SectionBuffer(document.contentFamily());

        for (DocumentBlock block : document.blocks()) {
            if (block.type() == BlockType.HEADING) {
                flushSection(document, options, sink, section);
                section.startHeading((TextBlock) block);
                continue;
            }
            if (block.type() == BlockType.PAGE_BREAK) {
                flushSection(document, options, sink, section);
                section.startPage((TextBlock) block);
                continue;
            }
            if (block.type() == BlockType.TABLE) {
                flushSection(document, options, sink, section);
                emitTable(document, options, sink, (TableBlock) block);
                continue;
            }
            if (block.type() == BlockType.FAQ) {
                flushSection(document, options, sink, section);
                emitFaq(document, options, sink, (FaqBlock) block);
                continue;
            }
            if (block.type() == BlockType.CODE) {
                flushSection(document, options, sink, section);
                emitCode(document, options, sink, (CodeBlock) block);
                continue;
            }
            section.append(block.asText(), block.metadata());
        }

        flushSection(document, options, sink, section);
        return sink.withPrevNextRelations();
    }

    private void flushSection(
            ParsedDocument document,
            ChunkingOptions options,
            ChunkSink sink,
            SectionBuffer section
    ) {
        if (section.isBlank()) {
            return;
        }
        if (document.contentFamily() == ContentFamily.CODE_OR_CONFIG) {
            emitCode(document, options, sink, new CodeBlock(section.language(), section.text(), section.metadata()));
        } else {
            emitTextSection(document, options, sink, section);
        }
        section.clearText();
    }

    private void emitTextSection(
            ParsedDocument document,
            ChunkingOptions options,
            ChunkSink sink,
            SectionBuffer section
    ) {
        Map<String, String> parentMetadata = new LinkedHashMap<>(section.metadata());
        parentMetadata.put("chunkTemplate", "parent-child");
        parentMetadata.put("strategy", "semantic-section");
        parentMetadata.put("contentFamilyStrategy", contentFamilyStrategy(document.contentFamily(), section.metadata()));
        if (!section.title().isBlank()) {
            parentMetadata.put("sectionTitle", section.title());
        }

        String sectionText = section.text();
        String parentText = parentText(section.title(), sectionText, options.parentMaxTokens());
        String parentId = sink.add(null, ChunkKind.PARENT, document.contentFamily(), parentText, parentMetadata);

        List<String> windows = sentenceWindows(sectionText, options);
        ChunkKind childKind = section.metadata().containsKey("pageNumber") ? ChunkKind.PDF_PAGE_SECTION : ChunkKind.SENTENCE_WINDOW;
        for (int i = 0; i < windows.size(); i++) {
            Map<String, String> metadata = new LinkedHashMap<>(section.metadata());
            metadata.put("strategy", childKind == ChunkKind.PDF_PAGE_SECTION ? "pdf-page-section-hybrid" : "semantic+sentence-window");
            metadata.put("windowIndex", String.valueOf(i));
            metadata.put("windowCount", String.valueOf(windows.size()));
            if (!section.title().isBlank()) {
                metadata.put("sectionTitle", section.title());
            }
            emitBudgetedText(sink, parentId, childKind, document.contentFamily(), windows.get(i), metadata, options.maxTokens());
        }
    }

    private void emitTable(
            ParsedDocument document,
            ChunkingOptions options,
            ChunkSink sink,
            TableBlock table
    ) {
        Map<String, String> parentMetadata = new LinkedHashMap<>(table.metadata());
        parentMetadata.put("chunkTemplate", "parent-child");
        parentMetadata.put("strategy", "table-summary");
        parentMetadata.put("rowCount", String.valueOf(table.rowCount()));
        parentMetadata.put("columnCount", String.valueOf(table.columnCount()));
        if (!table.caption().isBlank()) {
            parentMetadata.put("caption", table.caption());
        }
        String parentId = sink.add(null, ChunkKind.TABLE_SUMMARY, ContentFamily.STRUCTURED_TABLE, table.summary(), parentMetadata);

        List<String> currentRows = new ArrayList<>();
        int groupStart = -1;
        int groupEnd = -1;
        for (int rowIndex = 0; rowIndex < table.rowCount(); rowIndex++) {
            if (!rowHasData(table, rowIndex)) {
                continue;
            }
            String rowText = renderRow(table, rowIndex);
            String candidate = renderRowGroup(table, currentRows, rowText);
            if (!currentRows.isEmpty() && TokenEstimator.estimate(candidate) > options.maxTokens()) {
                emitTableRowGroup(sink, parentId, table, currentRows, groupStart, groupEnd, options);
                currentRows.clear();
                groupStart = -1;
            }
            if (groupStart < 0) {
                groupStart = rowIndex;
            }
            groupEnd = rowIndex;
            currentRows.add(rowText);
        }
        if (!currentRows.isEmpty()) {
            emitTableRowGroup(sink, parentId, table, currentRows, groupStart, groupEnd, options);
        }
    }

    private void emitTableRowGroup(
            ChunkSink sink,
            String parentId,
            TableBlock table,
            List<String> rows,
            int rowStart,
            int rowEnd,
            ChunkingOptions options
    ) {
        String text = renderRowGroup(table, rows, null);
        Map<String, String> metadata = new LinkedHashMap<>(table.metadata());
        metadata.put("strategy", "table-row-group");
        metadata.put("rowStart", String.valueOf(rowStart));
        metadata.put("rowEnd", String.valueOf(rowEnd));
        metadata.put("tableSummary", table.summary());
        emitBudgetedText(sink, parentId, ChunkKind.TABLE_ROW_GROUP, ContentFamily.STRUCTURED_TABLE, text, metadata, options.maxTokens());
    }

    private void emitFaq(
            ParsedDocument document,
            ChunkingOptions options,
            ChunkSink sink,
            FaqBlock faq
    ) {
        Map<String, String> metadata = new LinkedHashMap<>(faq.metadata());
        metadata.put("strategy", "faq-pair");
        metadata.put("question", faq.question());
        emitBudgetedText(sink, null, ChunkKind.FAQ_PAIR, document.contentFamily(), faq.asText(), metadata, options.maxTokens());
    }

    private void emitCode(
            ParsedDocument document,
            ChunkingOptions options,
            ChunkSink sink,
            CodeBlock code
    ) {
        List<String> structuralUnits = splitCodeByStructure(code.code(), options.maxTokens());
        for (int i = 0; i < structuralUnits.size(); i++) {
            Map<String, String> metadata = new LinkedHashMap<>(code.metadata());
            metadata.put("strategy", "code-ast-structural");
            metadata.put("unitIndex", String.valueOf(i));
            metadata.put("unitCount", String.valueOf(structuralUnits.size()));
            if (!code.language().isBlank()) {
                metadata.put("language", code.language());
            }
            emitBudgetedText(sink, null, ChunkKind.CODE_SYMBOL, document.contentFamily(), structuralUnits.get(i), metadata, options.maxTokens());
        }
    }

    private void emitBudgetedText(
            ChunkSink sink,
            String parentChunkId,
            ChunkKind kind,
            ContentFamily contentFamily,
            String text,
            Map<String, String> metadata,
            int maxTokens
    ) {
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            return;
        }
        if (TokenEstimator.estimate(normalized) <= maxTokens) {
            sink.add(parentChunkId, kind, contentFamily, normalized, metadata);
            return;
        }
        List<String> fallbackParts = splitByCharacterBudget(normalized, maxTokens);
        for (int i = 0; i < fallbackParts.size(); i++) {
            Map<String, String> fallbackMetadata = new LinkedHashMap<>(metadata);
            fallbackMetadata.put("fallback", "character");
            fallbackMetadata.put("fallbackIndex", String.valueOf(i));
            fallbackMetadata.put("fallbackCount", String.valueOf(fallbackParts.size()));
            sink.add(parentChunkId, kind, contentFamily, fallbackParts.get(i), fallbackMetadata);
        }
    }

    private List<String> sentenceWindows(String text, ChunkingOptions options) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return List.of();
        }
        List<String> windows = new ArrayList<>();
        int index = 0;
        while (index < sentences.size()) {
            int end = Math.min(sentences.size(), index + options.sentenceWindowSize());
            String window = String.join(" ", sentences.subList(index, end));
            if (TokenEstimator.estimate(window) <= options.maxTokens()) {
                windows.add(window);
            } else {
                windows.addAll(splitByCharacterBudget(window, options.maxTokens()));
            }
            if (end == sentences.size()) {
                break;
            }
            index = Math.max(index + 1, end - options.sentenceOverlap());
        }
        return windows;
    }

    private List<String> splitSentences(String text) {
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
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

    private void addSentence(List<String> sentences, StringBuilder current) {
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

    private List<String> splitByCharacterBudget(String text, int maxTokens) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int budget = Math.max(32, maxTokens);
        int currentTokens = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            int tokenCost = TokenEstimator.safeTokenCost(codePoint);
            if (!current.isEmpty() && currentTokens + tokenCost > budget) {
                parts.add(current.toString().trim());
                current.setLength(0);
                currentTokens = 0;
            }
            current.appendCodePoint(codePoint);
            currentTokens += tokenCost;
            offset += Character.charCount(codePoint);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private List<String> splitCodeByStructure(String code, int maxTokens) {
        String[] lines = code.split("\\R", -1);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            boolean boundary = isCodeBoundary(trimmed);
            if (boundary && !current.isEmpty() && TokenEstimator.estimate(current.toString()) >= maxTokens / 2) {
                chunks.add(current.toString().stripTrailing());
                current.setLength(0);
            }
            current.append(line).append('\n');
            if (TokenEstimator.estimate(current.toString()) > maxTokens) {
                chunks.addAll(splitByCharacterBudget(current.toString(), maxTokens));
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

    private String parentText(String title, String text, int parentMaxTokens) {
        String prefix = title == null || title.isBlank() ? "" : title + "\n";
        String normalized = normalizeText(prefix + text);
        if (TokenEstimator.estimate(normalized) <= parentMaxTokens) {
            return normalized;
        }
        return splitByCharacterBudget(normalized, parentMaxTokens).getFirst();
    }

    private boolean rowHasData(TableBlock table, int rowIndex) {
        for (TableCell cell : table.row(rowIndex)) {
            if (!cell.header() && !cell.value().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String renderRow(TableBlock table, int rowIndex) {
        StringJoiner joiner = new StringJoiner("; ");
        for (TableCell cell : table.row(rowIndex)) {
            if (cell.header()) {
                continue;
            }
            StringJoiner cellContext = new StringJoiner(" / ");
            for (Map.Entry<String, List<String>> entry : cell.inheritedHeaders().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    cellContext.add(entry.getKey() + "=" + String.join(" > ", entry.getValue()));
                }
            }
            String label = cellContext.length() == 0
                    ? "r" + (cell.rowIndex() + 1) + "c" + (cell.columnIndex() + 1)
                    : cellContext.toString();
            joiner.add(label + ": " + cell.value());
        }
        return "row " + (rowIndex + 1) + " | " + joiner;
    }

    private String renderRowGroup(TableBlock table, List<String> rows, String extraRow) {
        List<String> groupRows = new ArrayList<>(rows);
        if (extraRow != null) {
            groupRows.add(extraRow);
        }
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(table.summary());
        for (String row : groupRows) {
            joiner.add(row);
        }
        return joiner.toString();
    }

    private String contentFamilyStrategy(ContentFamily family, Map<String, String> metadata) {
        if (metadata.containsKey("pageNumber")) {
            return "pdf-page-section-hybrid";
        }
        return switch (family) {
            case CODE_OR_CONFIG -> "code-ast";
            case STRUCTURED_TABLE -> "table-row-group";
            case WEB_PAGE, RICH_TEXT -> "semantic-section";
            case PLAIN_TEXT -> "sentence-window";
            case SCANNED_DOCUMENT, IMAGE_TEXT -> "ocr-text-window";
            case PRESENTATION -> "slide-section";
        };
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.trim().replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n");
    }

    public enum ChunkKind {
        PARENT,
        SENTENCE_WINDOW,
        SEMANTIC,
        TABLE_SUMMARY,
        TABLE_ROW_GROUP,
        FAQ_PAIR,
        CODE_SYMBOL,
        PDF_PAGE_SECTION
    }

    public record ChunkingOptions(
            int maxTokens,
            int parentMaxTokens,
            int sentenceWindowSize,
            int sentenceOverlap
    ) {
        public ChunkingOptions {
            if (maxTokens < 64) {
                throw new IllegalArgumentException("maxTokens must be at least 64");
            }
            if (parentMaxTokens < maxTokens) {
                throw new IllegalArgumentException("parentMaxTokens must be greater than or equal to maxTokens");
            }
            if (sentenceWindowSize < 1) {
                throw new IllegalArgumentException("sentenceWindowSize must be positive");
            }
            if (sentenceOverlap < 0 || sentenceOverlap >= sentenceWindowSize) {
                throw new IllegalArgumentException("sentenceOverlap must be lower than sentenceWindowSize");
            }
        }

        public static ChunkingOptions defaults() {
            return new ChunkingOptions(512, 1_200, 4, 1);
        }
    }

    public record DocumentChunk(
            String chunkId,
            String parentChunkId,
            ChunkKind kind,
            ContentFamily contentFamily,
            String text,
            int tokenEstimate,
            Map<String, String> metadata,
            String previousChunkId,
            String nextChunkId
    ) {
        public DocumentChunk {
            chunkId = requireText(chunkId, "chunkId");
            kind = Objects.requireNonNull(kind, "kind");
            contentFamily = Objects.requireNonNull(contentFamily, "contentFamily");
            text = requireText(text, "text");
            tokenEstimate = Math.max(tokenEstimate, 1);
            metadata = metadata == null || metadata.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
            previousChunkId = previousChunkId == null ? "" : previousChunkId;
            nextChunkId = nextChunkId == null ? "" : nextChunkId;
        }

        private DocumentChunk withRelations(String previous, String next) {
            return new DocumentChunk(chunkId, parentChunkId, kind, contentFamily, text, tokenEstimate, metadata, previous, next);
        }
    }

    public static final class TokenEstimator {
        private TokenEstimator() {
        }

        public static int estimate(String text) {
            if (text == null || text.isBlank()) {
                return 0;
            }
            int tokens = 0;
            int latinRun = 0;
            for (int offset = 0; offset < text.length(); ) {
                int codePoint = text.codePointAt(offset);
                if (isCjk(codePoint)) {
                    tokens += flushLatin(latinRun) + 1;
                    latinRun = 0;
                } else if (Character.isLetterOrDigit(codePoint)) {
                    latinRun++;
                } else {
                    tokens += flushLatin(latinRun);
                    latinRun = 0;
                    if (!Character.isWhitespace(codePoint)) {
                        tokens++;
                    }
                }
                offset += Character.charCount(codePoint);
            }
            tokens += flushLatin(latinRun);
            return Math.max(tokens, 1);
        }

        private static int safeTokenCost(int codePoint) {
            if (Character.isWhitespace(codePoint)) {
                return 0;
            }
            return isCjk(codePoint) ? 1 : 1;
        }

        private static int flushLatin(int latinRun) {
            return latinRun == 0 ? 0 : Math.max(1, (latinRun + 3) / 4);
        }

        private static boolean isCjk(int codePoint) {
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            return script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL;
        }
    }

    private static final class ChunkSink {
        private final String documentId;
        private final List<DocumentChunk> chunks = new ArrayList<>();
        private int sequence;

        private ChunkSink(String documentId) {
            this.documentId = documentId;
        }

        private String add(
                String parentChunkId,
                ChunkKind kind,
                ContentFamily contentFamily,
                String text,
                Map<String, String> metadata
        ) {
            String id = documentId + "-chunk-" + String.format(Locale.ROOT, "%04d", ++sequence);
            chunks.add(new DocumentChunk(id, parentChunkId, kind, contentFamily, text, TokenEstimator.estimate(text), metadata, "", ""));
            return id;
        }

        private List<DocumentChunk> withPrevNextRelations() {
            List<DocumentChunk> related = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                String previous = i == 0 ? "" : chunks.get(i - 1).chunkId();
                String next = i == chunks.size() - 1 ? "" : chunks.get(i + 1).chunkId();
                related.add(chunks.get(i).withRelations(previous, next));
            }
            return Collections.unmodifiableList(related);
        }
    }

    private static final class SectionBuffer {
        private final ContentFamily contentFamily;
        private final StringBuilder text = new StringBuilder();
        private final Map<String, String> metadata = new LinkedHashMap<>();
        private String title = "";

        private SectionBuffer(ContentFamily contentFamily) {
            this.contentFamily = contentFamily;
        }

        private void startHeading(TextBlock heading) {
            title = heading.text();
            metadata.clear();
            metadata.putAll(heading.metadata());
        }

        private void startPage(TextBlock pageBreak) {
            metadata.clear();
            metadata.putAll(pageBreak.metadata());
            if (pageBreak.metadata().containsKey("pageNumber")) {
                title = "Page " + pageBreak.metadata().get("pageNumber");
            } else {
                title = pageBreak.text();
            }
        }

        private void append(String value, Map<String, String> blockMetadata) {
            if (value == null || value.isBlank()) {
                return;
            }
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(value.trim());
            if (blockMetadata != null) {
                for (Map.Entry<String, String> entry : blockMetadata.entrySet()) {
                    metadata.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }

        private boolean isBlank() {
            return text.toString().isBlank();
        }

        private String text() {
            return text.toString();
        }

        private String title() {
            return title;
        }

        private String language() {
            return metadata.getOrDefault("language", contentFamily == ContentFamily.CODE_OR_CONFIG ? "text" : "");
        }

        private Map<String, String> metadata() {
            return Collections.unmodifiableMap(metadata);
        }

        private void clearText() {
            text.setLength(0);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
