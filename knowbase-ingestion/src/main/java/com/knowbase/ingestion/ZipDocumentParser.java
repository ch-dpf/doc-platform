package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (lowerMime.contains("zip") || lowerMime.contains("compressed")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
        return lowerUri.endsWith(".zip");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try {
            byte[] content = source.inputStream().readAllBytes();
            Map<String, byte[]> entries = readZipEntries(content);
            List<String> markdownFiles = entries.keySet().stream()
                    .filter(ZipDocumentParser::isMarkdownEntry)
                    .sorted()
                    .toList();
            if (markdownFiles.isEmpty()) {
                throw new IllegalStateException("ZIP 包内未找到 Markdown 文档");
            }
            StringBuilder builder = new StringBuilder();
            List<String> embeddedImages = new ArrayList<>();
            for (String markdownPath : markdownFiles) {
                String markdown = new String(entries.get(markdownPath), StandardCharsets.UTF_8);
                String resolved = resolveMarkdownImages(markdown, markdownPath, entries, embeddedImages);
                if (!builder.isEmpty()) {
                    builder.append("\n\n---\n\n");
                }
                builder.append("# ").append(markdownPath).append('\n').append(resolved);
            }
            Map<String, Object> metadata = new HashMap<>(source.metadata());
            metadata.put("parser", "zip");
            metadata.put("zipEntryCount", entries.size());
            metadata.put("markdownEntryCount", markdownFiles.size());
            metadata.put("embeddedImageCount", embeddedImages.size());
            metadata.put("markdownEntries", markdownFiles);
            if (!embeddedImages.isEmpty()) {
                metadata.put("embeddedImages", List.copyOf(embeddedImages));
            }
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    builder.toString().trim(),
                    ContentFamily.RICH_TEXT,
                    Map.copyOf(metadata)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("ZIP 文档解析失败: " + source.sourceUri(), exception);
        }
    }

    private static Map<String, byte[]> readZipEntries(byte[] content) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(normalizeEntryPath(entry.getName()), zipInputStream.readAllBytes());
                }
                zipInputStream.closeEntry();
            }
        }
        return entries;
    }

    private static String resolveMarkdownImages(
            String markdown,
            String markdownPath,
            Map<String, byte[]> entries,
            List<String> embeddedImages
    ) {
        String baseDir = parentDir(markdownPath);
        String resolved = markdown;
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String entryPath = entry.getKey();
            if (!isImageEntry(entryPath)) {
                continue;
            }
            String fileName = fileName(entryPath);
            String relativePath = baseDir.isBlank() ? fileName : baseDir + "/" + fileName;
            if (resolved.contains(fileName) || resolved.contains(relativePath) || resolved.contains(entryPath)) {
                embeddedImages.add(entryPath);
                resolved = resolved
                        .replace("](" + fileName + ")", "](" + entryPath + ")")
                        .replace("](" + relativePath + ")", "](" + entryPath + ")")
                        .replace("](" + entryPath + ")", "][image:" + entryPath + "]");
            }
        }
        return resolved;
    }

    private static boolean isMarkdownEntry(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    private static boolean isImageEntry(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp")
                || lower.endsWith(".svg");
    }

    private static String normalizeEntryPath(String path) {
        return path.replace('\\', '/').replaceAll("^\\./+", "");
    }

    private static String parentDir(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }
}
