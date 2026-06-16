package com.knowbase.pipeline.content;

import com.knowbase.vector.chunk.ChunkingStrategy;

import java.util.Map;

/** 各文件类型在「自动」分块策略下的代码默认策略（与 FILE-TYPE-PROCESSING 附录 A 对齐）。 */
public final class FileTypeChunkStrategyDefaults {

    private static final Map<String, ChunkingStrategy> BY_FILE_TYPE = Map.of(
            "pdf", ChunkingStrategy.PARAGRAPH_FIRST,
            "word", ChunkingStrategy.HEADING_LEVEL,
            "txt", ChunkingStrategy.PARAGRAPH_FIRST,
            "markdown", ChunkingStrategy.HEADING_LEVEL,
            "excel", ChunkingStrategy.PARAGRAPH_FIRST);

    private FileTypeChunkStrategyDefaults() {
    }

    public static ChunkingStrategy forFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return ChunkingStrategy.PARAGRAPH_FIRST;
        }
        return BY_FILE_TYPE.getOrDefault(fileType.trim().toLowerCase(), ChunkingStrategy.PARAGRAPH_FIRST);
    }
}
