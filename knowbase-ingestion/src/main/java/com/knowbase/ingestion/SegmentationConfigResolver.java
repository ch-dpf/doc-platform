package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SegmentationConfigResolver {

    private SegmentationConfigResolver() {
    }

    public static SegmentationConfig resolve(
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            Map<String, Object> requestOptions
    ) {
        Map<String, Object> merged = mergeOptions(documentProfile, requestOptions);
        int chunkMaxTokens = readInt(merged, "chunkMaxTokens", libraryProfile.chunkMaxTokens());
        int chunkOverlapTokens = readInt(merged, "chunkOverlapTokens", libraryProfile.chunkOverlapTokens());
        String chunkingStrategy = stringOption(merged, "chunkingStrategy");
        if (chunkingStrategy == null && documentProfile != null) {
            chunkingStrategy = documentProfile.chunkingStrategy();
        }
        if (chunkingStrategy == null) {
            chunkingStrategy = "structure_token_window";
        }

        SegmentationConfig defaults = SegmentationConfig.defaults(
                chunkMaxTokens,
                chunkOverlapTokens,
                chunkingStrategy
        );

        SegmentationConfig.ChunkMode chunkMode = parseChunkMode(merged, defaults.chunkMode());
        SegmentationConfig.SplitMode splitMode = parseSplitMode(merged, defaults.splitMode());
        List<String> separators = parseSeparators(merged, defaults.separators());
        boolean preserveBoundary = readBoolean(merged, "preserveStructureBoundary", defaults.preserveStructureBoundary());
        boolean prependHeading = readBoolean(merged, "prependHeadingContext", defaults.prependHeadingContext());
        SegmentationConfig.SizeUnit sizeUnit = parseSizeUnit(merged, defaults.sizeUnit());
        int chunkMaxChars = readInt(merged, "chunkMaxChars", defaults.chunkMaxChars());
        int chunkOverlapChars = readInt(merged, "chunkOverlapChars", defaults.chunkOverlapChars());
        int minChunkChars = readInt(merged, "minChunkChars", defaults.minChunkChars());

        return new SegmentationConfig(
                chunkMode,
                splitMode,
                sizeUnit,
                separators,
                preserveBoundary,
                prependHeading,
                chunkMaxTokens,
                chunkOverlapTokens,
                chunkMaxChars,
                chunkOverlapChars,
                minChunkChars,
                chunkingStrategy
        );
    }

    public static Map<String, Object> mergeOptions(DocumentProfile documentProfile, Map<String, Object> requestOptions) {
        HashMap<String, Object> merged = new HashMap<>();
        if (documentProfile != null && documentProfile.options() != null) {
            merged.putAll(documentProfile.options());
        }
        if (requestOptions != null) {
            merged.putAll(requestOptions);
        }
        if (SegmentationOptionsSupport.isSmartMode(requestOptions)) {
            merged.put("chunkMode", "flat");
            merged.put("splitMode", "recursive");
            merged.put("chunkSizeUnit", "char");
            merged.put("chunkMaxChars", SegmentationOptionsSupport.SMART_CHUNK_MAX_CHARS);
            merged.put("chunkOverlapChars", SegmentationOptionsSupport.SMART_CHUNK_OVERLAP_CHARS);
            merged.putIfAbsent("minChunkChars", 80);
            merged.put("prependHeadingContext", true);
            merged.putIfAbsent("preserveStructureBoundary", true);
        }
        return Map.copyOf(merged);
    }

    private static SegmentationConfig.SizeUnit parseSizeUnit(Map<String, Object> options, SegmentationConfig.SizeUnit defaultValue) {
        String value = stringOption(options, "chunkSizeUnit");
        if (value == null) {
            value = stringOption(options, "sizeUnit");
        }
        if (value == null) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "token", "tokens" -> SegmentationConfig.SizeUnit.TOKEN;
            default -> SegmentationConfig.SizeUnit.CHAR;
        };
    }

    private static SegmentationConfig.ChunkMode parseChunkMode(Map<String, Object> options, SegmentationConfig.ChunkMode defaultValue) {
        String value = stringOption(options, "chunkMode");
        if (value == null) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "parent_child", "parent-child", "hierarchical" -> SegmentationConfig.ChunkMode.PARENT_CHILD;
            default -> SegmentationConfig.ChunkMode.FLAT;
        };
    }

    private static SegmentationConfig.SplitMode parseSplitMode(Map<String, Object> options, SegmentationConfig.SplitMode defaultValue) {
        String value = stringOption(options, "splitMode");
        if (value == null) {
            String fallback = stringOption(options, "fallbackSplitMode");
            if ("token_window".equalsIgnoreCase(fallback)) {
                return SegmentationConfig.SplitMode.STRUCTURE_ONLY;
            }
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "structure", "structure_only", "structure-only" -> SegmentationConfig.SplitMode.STRUCTURE_ONLY;
            default -> SegmentationConfig.SplitMode.RECURSIVE;
        };
    }

    private static List<String> parseSeparators(Map<String, Object> options, List<String> defaultSeparators) {
        Object configured = options.get("separators");
        if (configured instanceof List<?> list && !list.isEmpty()) {
            List<String> separators = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    separators.add(unescapeSeparator(String.valueOf(item)));
                }
            }
            if (!separators.isEmpty()) {
                return List.copyOf(separators);
            }
        }
        String custom = stringOption(options, "customSeparators");
        if (custom != null) {
            List<String> separators = new ArrayList<>();
            for (String part : custom.split("\\|")) {
                if (!part.isBlank()) {
                    separators.add(unescapeSeparator(part.trim()));
                }
            }
            if (!separators.isEmpty()) {
                return List.copyOf(separators);
            }
        }
        return defaultSeparators;
    }

    private static String unescapeSeparator(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r");
    }

    private static int readInt(Map<String, Object> options, String key, int defaultValue) {
        Object configured = options.get(key);
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

    private static boolean readBoolean(Map<String, Object> options, String key, boolean defaultValue) {
        Object value = options.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value).trim());
        }
        return defaultValue;
    }

    private static String stringOption(Map<String, Object> options, String key) {
        Object value = options.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
