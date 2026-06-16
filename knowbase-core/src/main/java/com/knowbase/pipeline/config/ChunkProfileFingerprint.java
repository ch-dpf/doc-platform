package com.knowbase.pipeline.config;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.config.ChunkingProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 分块配置档指纹：入库与库默认主档均由此生成稳定 {@code cp_*} 标识。
 */
public final class ChunkProfileFingerprint {

    public static final String METADATA_FIELD = "chunkProfileId";

    private ChunkProfileFingerprint() {}

    public static String compute(UUID libraryId, EffectivePipelineConfig effective) {
        return computeFromChunking(libraryId, effective.chunking());
    }

    public static String computeLibraryPrimary(UUID libraryId, VectorLibraryConfig library) {
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategyResolver.resolve(
                library.getChunkingStrategy(), "application/pdf"));
        chunking.setChunkSize(library.getChunkSize());
        chunking.setOverlap(library.getChunkOverlap());
        chunking.setMinParagraphLength(library.getMinParagraphLength());
        chunking.setHierarchicalChunkingEnabled(library.isHierarchicalChunkingEnabled());
        chunking.setCustomDelimiter(library.getChunkDelimiter());
        return computeFromChunking(libraryId, chunking);
    }

    static String computeFromChunking(UUID libraryId, ChunkingProperties chunking) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("libraryId", libraryId.toString());
        payload.put("strategy", chunking.getStrategy().name());
        payload.put("chunkSize", chunking.getChunkSize());
        payload.put("overlap", chunking.getOverlap());
        payload.put("minParagraphLength", chunking.getMinParagraphLength());
        payload.put("hierarchical", chunking.isHierarchicalChunkingEnabled());
        payload.put("delimiter", normalizeDelimiter(chunking.getCustomDelimiter()));
        return "cp_" + sha256Hex12(JsonSupport.toJson(payload));
    }

    private static String normalizeDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isBlank()) {
            return "";
        }
        return delimiter;
    }

    private static String sha256Hex12(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
