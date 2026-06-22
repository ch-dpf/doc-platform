package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;

import java.util.HashMap;
import java.util.Map;

public final class SegmentationOptionsSupport {

    public static final String MODE_SMART = "smart";
    public static final String MODE_ADVANCED = "advanced";
    /** 主流 RAG 智能分段默认：结构段内 500 字窗口 + 50 重叠（Dify/LangChain 量级） */
    public static final int SMART_CHUNK_MAX_CHARS = 500;
    public static final int SMART_CHUNK_OVERLAP_CHARS = 50;

    private SegmentationOptionsSupport() {
    }

    public static LibraryProfile applyLibraryProfileOverrides(LibraryProfile profile, Map<String, Object> options) {
        if (profile == null || !isAdvancedMode(options)) {
            return profile;
        }
        int chunkMaxTokens = readInt(options, "chunkMaxTokens", profile.chunkMaxTokens());
        int chunkOverlapTokens = readInt(options, "chunkOverlapTokens", profile.chunkOverlapTokens());
        if (chunkMaxTokens == profile.chunkMaxTokens() && chunkOverlapTokens == profile.chunkOverlapTokens()) {
            return profile;
        }
        return new LibraryProfile(
                profile.profileId(),
                profile.libraryId(),
                profile.version(),
                profile.embeddingProvider(),
                profile.embeddingModel(),
                profile.embeddingDimension(),
                profile.embeddingTokenizerProfileId(),
                chunkMaxTokens,
                chunkOverlapTokens,
                profile.retrievalTopK(),
                profile.options(),
                profile.createdAt()
        );
    }

    public static DocumentProfile applyDocumentProfileOverrides(DocumentProfile profile, Map<String, Object> options) {
        if (profile == null) {
            return null;
        }
        Map<String, Object> mergedOptions = new HashMap<>(SegmentationConfigResolver.mergeOptions(profile, options));
        String chunkingStrategy = stringOption(options, "chunkingStrategy");
        if (chunkingStrategy != null) {
            mergedOptions.put("chunkingStrategy", chunkingStrategy);
        }
        if (SegmentationOptionsSupport.isAdvancedMode(options)) {
            mergeAdvancedOption(mergedOptions, options, "preserveStructureBoundary");
            mergeAdvancedOption(mergedOptions, options, "chunkMode");
            mergeAdvancedOption(mergedOptions, options, "splitMode");
            mergeAdvancedOption(mergedOptions, options, "prependHeadingContext");
            mergeAdvancedOption(mergedOptions, options, "chunkSizeUnit");
            mergeAdvancedOption(mergedOptions, options, "chunkMaxChars");
            mergeAdvancedOption(mergedOptions, options, "chunkOverlapChars");
            mergeAdvancedOption(mergedOptions, options, "minChunkChars");
            mergeAdvancedOption(mergedOptions, options, "customSeparators");
            mergeAdvancedOption(mergedOptions, options, "separators");
        }
        String resolvedStrategy = chunkingStrategy == null ? profile.chunkingStrategy() : chunkingStrategy;
        if (resolvedStrategy.equals(profile.chunkingStrategy()) && optionsEqual(profile.options(), mergedOptions)) {
            return profile;
        }
        return new DocumentProfile(
                profile.documentProfileId(),
                profile.libraryId(),
                profile.code(),
                profile.contentFamily(),
                profile.parserCode(),
                resolvedStrategy,
                profile.tokenizerProfileId(),
                profile.metadataSchema(),
                Map.copyOf(mergedOptions),
                profile.enabled()
        );
    }

    private static void mergeAdvancedOption(Map<String, Object> merged, Map<String, Object> options, String key) {
        if (options != null && options.containsKey(key)) {
            merged.put(key, options.get(key));
        }
    }

    private static boolean optionsEqual(Map<String, Object> left, Map<String, Object> right) {
        return left == null ? right == null || right.isEmpty() : left.equals(right);
    }

    public static String resolveDocumentProfileCode(String requestedProfileCode, Map<String, Object> options) {
        if (isAdvancedMode(options)) {
            return requestedProfileCode;
        }
        return null;
    }

    public static boolean isAdvancedMode(Map<String, Object> options) {
        return MODE_ADVANCED.equalsIgnoreCase(stringOption(options, "segmentationMode"));
    }

    public static boolean isSmartMode(Map<String, Object> options) {
        String mode = stringOption(options, "segmentationMode");
        return mode == null || mode.isBlank() || MODE_SMART.equalsIgnoreCase(mode);
    }

    private static int readInt(Map<String, Object> options, String key, int defaultValue) {
        Object configured = options == null ? null : options.get(key);
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

    private static String stringOption(Map<String, Object> options, String key) {
        if (options == null) {
            return null;
        }
        Object value = options.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
