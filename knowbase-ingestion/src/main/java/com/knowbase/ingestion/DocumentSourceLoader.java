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

    public ParsedDocument load(String sourceUri, Map<String, Object> options) {
        LoadedContent loaded = resolveContent(sourceUri, options);
        String preferredParser = options == null ? null : stringOption(options.get("parserCode"));
        DocumentParser parser = selectParser(sourceUri, loaded.mimeType(), preferredParser)
                .orElseThrow(() -> new IllegalArgumentException("不支持的文档来源: " + sourceUri));
        return parser.parse(new DocumentSource(
                sourceUri,
                loaded.filename(),
                loaded.mimeType(),
                new java.io.ByteArrayInputStream(loaded.content()),
                loaded.metadata()
        ));
    }

    private Optional<DocumentParser> selectParser(String sourceUri, String mimeType, String preferredParser) {
        if (preferredParser != null) {
            return parsers.stream()
                    .filter(candidate -> parserMatches(candidate, preferredParser))
                    .filter(candidate -> candidate.supports(sourceUri, mimeType))
                    .findFirst()
                    .or(() -> "tika".equals(preferredParser.trim().toLowerCase())
                            ? parsers.stream().filter(TikaDocumentParser.class::isInstance).findFirst()
                            : Optional.empty());
        }
        return parsers.stream()
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
        if ("ocr".equals(normalized)) {
            return parser instanceof OcrDocumentParser;
        }
        if ("table-deep".equals(normalized) || "table".equals(normalized)) {
            return parser instanceof StructuredTableDocumentParser;
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
