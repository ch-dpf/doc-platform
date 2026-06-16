package com.knowbase.pipeline.config;

import com.knowbase.ingest.parse.ParserRuleResolver;
import com.knowbase.pipeline.content.FileTypeChunkStrategyDefaults;
import com.knowbase.vector.chunk.ChunkingStrategy;

/** 解析库级分块策略选择与 MIME/fileType 默认。 */
public final class ChunkingStrategyResolver {

    private ChunkingStrategyResolver() {
    }

    public static ChunkingStrategy resolve(ChunkingStrategy libraryStrategy, String mimeType) {
        if (libraryStrategy == null || libraryStrategy == ChunkingStrategy.AUTO) {
            String fileType = ParserRuleResolver.resolveFileType(mimeType, null);
            return FileTypeChunkStrategyDefaults.forFileType(fileType);
        }
        return libraryStrategy;
    }

    public static ChunkingStrategy effectiveForFileType(ChunkingStrategy libraryStrategy, String fileType) {
        if (libraryStrategy == null || libraryStrategy == ChunkingStrategy.AUTO) {
            return FileTypeChunkStrategyDefaults.forFileType(fileType);
        }
        return libraryStrategy;
    }
}
