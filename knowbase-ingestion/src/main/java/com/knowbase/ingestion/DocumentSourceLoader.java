package com.knowbase.ingestion;

import com.knowbase.storage.ObjectStorage;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DocumentSourceLoader {

    private final ObjectStorage objectStorage;
    private final List<DocumentParser> parsers;

    public DocumentSourceLoader(ObjectStorage objectStorage, List<DocumentParser> parsers) {
        this.objectStorage = objectStorage;
        this.parsers = List.copyOf(parsers);
    }

    public SourceContent loadSourceContent(String sourceUri) {
        LoadedContent loaded = resolveContent(sourceUri, Map.of());
        return new SourceContent(loaded.filename(), loaded.mimeType(), loaded.content());
    }

    public record SourceContent(String filename, String mimeType, byte[] content) {
    }

    public ParsedDocument load(String sourceUri, Map<String, Object> options) {
        LoadedContent loaded = resolveContent(sourceUri, options);
        Map<String, Object> effectiveOptions = options == null ? Map.of() : options;
        Map<String, Object> routedOptions = ParseOptionsSupport.applyParseMode(effectiveOptions, sourceUri);
        String preferredParser = stringOption(routedOptions.get("parserCode"));
        DocumentParser parser = selectParser(sourceUri, loaded.mimeType(), preferredParser, routedOptions)
                .orElseThrow(() -> new IllegalArgumentException("不支持的文档来源: " + sourceUri));
        HashMap<String, Object> metadata = new HashMap<>(loaded.metadata());
        metadata.putAll(routedOptions);
        return parser.parse(new DocumentSource(
                sourceUri,
                loaded.filename(),
                loaded.mimeType(),
                new java.io.ByteArrayInputStream(loaded.content()),
                Map.copyOf(metadata)
        ));
    }

    private Optional<DocumentParser> selectParser(
            String sourceUri,
            String mimeType,
            String preferredParser,
            Map<String, Object> options
    ) {
        if (preferredParser != null) {
            if (isExternalParser(preferredParser) && stringOption(options.get("externalParserEndpoint")) != null) {
                Optional<DocumentParser> external = parsers.stream()
                        .filter(ExternalDocumentParser.class::isInstance)
                        .findFirst();
                if (external.isPresent()) {
                    return external;
                }
            }
            Optional<DocumentParser> preferred = parsers.stream()
                    .filter(candidate -> parserMatches(candidate, preferredParser))
                    .filter(candidate -> candidate.supports(sourceUri, mimeType))
                    .findFirst();
            if (preferred.isPresent()) {
                return preferred;
            }
            if ("tika".equals(preferredParser.trim().toLowerCase())) {
                return parsers.stream().filter(TikaDocumentParser.class::isInstance).findFirst();
            }
        }
        Optional<DocumentParser> qaParser = parsers.stream()
                .filter(QaDocumentParser.class::isInstance)
                .map(QaDocumentParser.class::cast)
                .filter(candidate -> candidate.supportsExplicit(sourceUri, mimeType, null))
                .map(DocumentParser.class::cast)
                .findFirst();
        if (qaParser.isPresent()) {
            return qaParser;
        }
        if (ParseOptionsSupport.isOcrMode(options)) {
            Optional<DocumentParser> ocrLayout = parsers.stream()
                    .filter(OcrLayoutDocumentParser.class::isInstance)
                    .filter(candidate -> candidate.supports(sourceUri, mimeType))
                    .findFirst();
            if (ocrLayout.isPresent()) {
                return ocrLayout;
            }
        }
        if (ParseOptionsSupport.isLayoutMode(options)) {
            Optional<DocumentParser> layoutParser = parsers.stream()
                    .filter(PdfLayoutParser.class::isInstance)
                    .filter(candidate -> candidate.supports(sourceUri, mimeType))
                    .findFirst();
            if (layoutParser.isPresent()) {
                return layoutParser;
            }
        }
        Optional<DocumentParser> structureParser = structureParserFor(sourceUri, mimeType, options);
        if (structureParser.isPresent()) {
            return structureParser;
        }
        return parsers.stream()
                .filter(candidate -> candidate.supports(sourceUri, mimeType))
                .findFirst();
    }

    private static boolean isExternalParser(String parserCode) {
        String normalized = parserCode.trim().toLowerCase();
        return "docling".equals(normalized) || "unstructured".equals(normalized) || "external".equals(normalized);
    }

    private Optional<DocumentParser> structureParserFor(
            String sourceUri,
            String mimeType,
            Map<String, Object> options
    ) {
        return parsers.stream()
                .filter(candidate -> candidate instanceof MarkdownStructureParser
                        || candidate instanceof HtmlStructureParser
                        || candidate instanceof DocxStructureParser
                        || candidate instanceof PptxStructureParser
                        || candidate instanceof PdfLayoutParser
                        || candidate instanceof PdfStructureParser
                        || candidate instanceof TextStructureParser
                        || candidate instanceof OcrLayoutDocumentParser)
                .filter(candidate -> !(candidate instanceof PdfStructureParser
                        && ParseOptionsSupport.isLayoutMode(options)))
                .filter(candidate -> candidate.supports(sourceUri, mimeType))
                .findFirst();
    }

    private LoadedContent resolveContent(String sourceUri, Map<String, Object> options) {
        if (sourceUri.startsWith("inline:text:")) {
            String text = sourceUri.substring("inline:text:".length());
            String mimeType = "text/markdown";
            byte[] content = text.getBytes(StandardCharsets.UTF_8);
            return new LoadedContent("inline.md", mimeType, content, sourceMetadata("inline.md", mimeType, content.length, "inline"));
        }
        if (sourceUri.startsWith("minio://")) {
            String remainder = sourceUri.substring("minio://".length());
            int slash = remainder.indexOf('/');
            if (slash <= 0) {
                throw new IllegalArgumentException("MinIO URI 格式无效: " + sourceUri);
            }
            String bucket = remainder.substring(0, slash);
            String objectKey = remainder.substring(slash + 1);
            try (InputStream inputStream = objectStorage.get(bucket, objectKey)) {
                byte[] content = inputStream.readAllBytes();
                String filename = objectKey.contains("/") ? objectKey.substring(objectKey.lastIndexOf('/') + 1) : objectKey;
                String mimeType = guessMimeType(filename);
                Map<String, Object> metadata = sourceMetadata(filename, mimeType, content.length, "minio");
                metadata = new HashMap<>(metadata);
                metadata.put("bucket", bucket);
                metadata.put("objectKey", objectKey);
                return new LoadedContent(filename, mimeType, content, Map.copyOf(metadata));
            } catch (Exception exception) {
                throw new IllegalStateException("读取 MinIO 文档失败: " + sourceUri, exception);
            }
        }
        if (sourceUri.startsWith("inline://")) {
            String objectKey = sourceUri.substring("inline://".length());
            try (InputStream inputStream = objectStorage.get("inline", objectKey)) {
                byte[] content = inputStream.readAllBytes();
                String mimeType = guessMimeType(objectKey);
                return new LoadedContent(objectKey, mimeType, content, sourceMetadata(objectKey, mimeType, content.length, "object_storage"));
            } catch (Exception exception) {
                throw new IllegalStateException("读取内联文档失败: " + sourceUri, exception);
            }
        }
        if (sourceUri.startsWith("file://")) {
            try {
                String pathText = URLDecoder.decode(sourceUri.substring("file://".length()), StandardCharsets.UTF_8);
                Path path = Paths.get(pathText);
                byte[] content = Files.readAllBytes(path);
                String filename = path.getFileName().toString();
                String mimeType = guessMimeType(filename);
                Map<String, Object> metadata = sourceMetadata(filename, mimeType, content.length, "file");
                metadata = new HashMap<>(metadata);
                metadata.put("absolutePath", path.toAbsolutePath().toString());
                return new LoadedContent(filename, mimeType, content, Map.copyOf(metadata));
            } catch (Exception exception) {
                throw new IllegalStateException("读取本地文件失败: " + sourceUri, exception);
            }
        }
        if (options != null && options.get("content") instanceof String text && !text.isBlank()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fromOptions", true);
            return new LoadedContent("options.md", "text/markdown", text.getBytes(StandardCharsets.UTF_8), metadata);
        }
        throw new IllegalArgumentException("当前支持 inline:text:, minio://, inline://, file:// 或 options.content 文档来源: " + sourceUri);
    }

    private static String guessMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "text/markdown";
        }
        if (lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".csv")) {
            if (lower.endsWith(".csv")) {
                return "text/csv";
            }
            return "text/plain";
        }
        if (lower.endsWith(".rtf")) {
            return "application/rtf";
        }
        if (lower.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        }
        if (lower.endsWith(".ods")) {
            return "application/vnd.oasis.opendocument.spreadsheet";
        }
        if (lower.endsWith(".odp")) {
            return "application/vnd.oasis.opendocument.presentation";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
            return "image/tiff";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".zip")) {
            return "application/zip";
        }
        return "text/plain";
    }

    private record LoadedContent(String filename, String mimeType, byte[] content, Map<String, Object> metadata) {
    }

    private static String stringOption(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private static boolean parserMatches(DocumentParser parser, String parserCode) {
        String normalized = parserCode.trim().toLowerCase();
        if ("text".equals(normalized)) {
            return parser instanceof TextDocumentParser;
        }
        if ("tika".equals(normalized)) {
            return parser instanceof TikaDocumentParser;
        }
        if ("ocr".equals(normalized) || "ocr-layout".equals(normalized)) {
            return parser instanceof OcrDocumentParser || parser instanceof OcrLayoutDocumentParser;
        }
        if ("table-deep".equals(normalized) || "table".equals(normalized)) {
            return parser instanceof StructuredTableDocumentParser;
        }
        if ("qa".equals(normalized)) {
            return parser instanceof QaDocumentParser;
        }
        if ("zip".equals(normalized)) {
            return parser instanceof ZipDocumentParser;
        }
        if ("markdown-structure".equals(normalized)) {
            return parser instanceof MarkdownStructureParser;
        }
        if ("html-structure".equals(normalized)) {
            return parser instanceof HtmlStructureParser;
        }
        if ("docx-structure".equals(normalized)) {
            return parser instanceof DocxStructureParser;
        }
        if ("pptx-structure".equals(normalized)) {
            return parser instanceof PptxStructureParser;
        }
        if ("pdf-structure".equals(normalized)) {
            return parser instanceof PdfStructureParser;
        }
        if ("pdf-layout".equals(normalized)) {
            return parser instanceof PdfLayoutParser;
        }
        if ("text-structure".equals(normalized)) {
            return parser instanceof TextStructureParser;
        }
        if ("docling".equals(normalized) || "unstructured".equals(normalized) || "external".equals(normalized)) {
            return parser instanceof ExternalDocumentParser;
        }
        return false;
    }

    private static Map<String, Object> sourceMetadata(String filename, String mimeType, int size, String sourceType) {
        return Map.of(
                "filename", filename,
                "mimeType", mimeType,
                "fileExtension", extension(filename),
                "sourceType", sourceType,
                "sourceSizeBytes", size
        );
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
