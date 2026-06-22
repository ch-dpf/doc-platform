package com.knowbase.ingestion;

import java.util.List;

public record SegmentationConfig(
        ChunkMode chunkMode,
        SplitMode splitMode,
        SizeUnit sizeUnit,
        List<String> separators,
        boolean preserveStructureBoundary,
        boolean prependHeadingContext,
        int chunkMaxTokens,
        int chunkOverlapTokens,
        int chunkMaxChars,
        int chunkOverlapChars,
        int minChunkChars,
        String chunkingStrategy
) {

    public enum ChunkMode {
        FLAT,
        PARENT_CHILD
    }

    public enum SplitMode {
        RECURSIVE,
        STRUCTURE_ONLY
    }

    public enum SizeUnit {
        TOKEN,
        CHAR
    }

    public int effectiveMaxChars() {
        if (sizeUnit == SizeUnit.CHAR) {
            return chunkMaxChars;
        }
        return Math.max(256, chunkMaxTokens * 4);
    }

    public static SegmentationConfig defaults(int chunkMaxTokens, int chunkOverlapTokens, String chunkingStrategy) {
        return new SegmentationConfig(
                ChunkMode.FLAT,
                SplitMode.RECURSIVE,
                SizeUnit.TOKEN,
                RecursiveCharacterSplitter.defaultSeparators(),
                true,
                true,
                chunkMaxTokens,
                chunkOverlapTokens,
                Math.max(256, chunkMaxTokens * 4),
                Math.max(32, chunkOverlapTokens * 4),
                80,
                chunkingStrategy
        );
    }
}
