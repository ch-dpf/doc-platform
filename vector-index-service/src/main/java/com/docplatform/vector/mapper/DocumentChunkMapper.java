package com.docplatform.vector.mapper;

import com.docplatform.vector.dto.SearchHit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DocumentChunkMapper {

    void deleteByDocId(@Param("docId") UUID docId);

    void deleteByDocIdAndVersion(@Param("docId") UUID docId, @Param("version") int version);

    void insertChunk(
            @Param("chunkId") UUID chunkId,
            @Param("docId") UUID docId,
            @Param("tenantId") String tenantId,
            @Param("version") int version,
            @Param("chunkIndex") int chunkIndex,
            @Param("content") String content,
            @Param("metadataJson") String metadataJson,
            @Param("embedding") float[] embedding);

    List<SearchHit> search(
            @Param("tenantId") String tenantId,
            @Param("embedding") float[] queryEmbedding,
            @Param("topK") int topK,
            @Param("docIds") List<UUID> docIds);
}
