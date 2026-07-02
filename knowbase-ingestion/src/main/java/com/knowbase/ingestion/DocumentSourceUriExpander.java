package com.knowbase.ingestion;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class DocumentSourceUriExpander {

    private static final int DEFAULT_MAX_FILES = 200;
    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(
            "md", "markdown", "txt", "log", "csv",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip",
            "html", "htm", "json", "xml", "yml", "yaml", "properties",
            "java", "kt", "js", "ts", "vue", "py"
    );

    public List<String> expand(List<String> sourceUris, Map<String, Object> options) {
        if (sourceUris == null || sourceUris.isEmpty()) {
            return List.of();
        }
        int maxFiles = readInt(options, "maxFiles", DEFAULT_MAX_FILES);
        Set<String> extensions = readExtensions(options);
        return sourceUris.stream()
                .flatMap(sourceUri -> expandOne(sourceUri, options, extensions).stream())
                .distinct()
                .limit(Math.max(1, maxFiles))
                .toList();
    }

    private List<String> expandOne(String sourceUri, Map<String, Object> options, Set<String> extensions) {
        if (sourceUri == null || sourceUri.isBlank() || !sourceUri.startsWith("file://")) {
            return sourceUri == null || sourceUri.isBlank() ? List.of() : List.of(sourceUri);
        }
        Path path = Paths.get(URLDecoder.decode(sourceUri.substring("file://".length()), StandardCharsets.UTF_8));
        if (!Files.isDirectory(path)) {
            return List.of(sourceUri);
        }
        int depth = Boolean.FALSE.equals(value(options, "recursive")) ? 1 : Integer.MAX_VALUE;
        try (Stream<Path> walk = Files.walk(path, depth)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(file -> extensions.contains(extension(file)))
                    .sorted(Comparator.comparing(file -> file.toAbsolutePath().toString()))
                    .map(DocumentSourceUriExpander::toFileUri)
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("扫描本地目录失败: " + sourceUri, exception);
        }
    }

    private static Set<String> readExtensions(Map<String, Object> options) {
        Object configured = value(options, "extensions");
        if (configured instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .map(String::valueOf)
                    .map(DocumentSourceUriExpander::normalizeExtension)
                    .filter(item -> !item.isBlank())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        if (configured instanceof String text && !text.isBlank()) {
            return Stream.of(text.split(","))
                    .map(DocumentSourceUriExpander::normalizeExtension)
                    .filter(item -> !item.isBlank())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
        return DEFAULT_EXTENSIONS;
    }

    private static int readInt(Map<String, Object> options, String key, int defaultValue) {
        Object configured = value(options, key);
        if (configured instanceof Number number) {
            return number.intValue();
        }
        if (configured != null) {
            try {
                return Integer.parseInt(String.valueOf(configured));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Object value(Map<String, Object> options, String key) {
        return options == null ? null : options.get(key);
    }

    private static String extension(Path path) {
        return normalizeExtension(path.getFileName().toString());
    }

    private static String normalizeExtension(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        int dot = normalized.lastIndexOf('.');
        if (dot >= 0) {
            normalized = normalized.substring(dot + 1);
        }
        return normalized;
    }

    private static String toFileUri(Path path) {
        return "file://" + path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
