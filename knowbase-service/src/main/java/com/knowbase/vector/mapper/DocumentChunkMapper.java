package com.knowbase.vector.mapper;

import com.knowbase.vector.dto.DocChunkCountRow;
import com.knowbase.vector.dto.DocVersionPair;
import com.knowbase.vector.dto.DocumentChunkRow;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.retrieval.MetadataFilterClause;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DocumentChunkMapper {

    void deleteByDocId(@Param("docId") UUID docId);

    void deleteByDocIdAndVersion(@Param("docId") UUID docId, @Param("version") int version);

    void deleteByLibraryId(@Param("libraryId") UUID libraryId);

    int countByLibraryId(@Param("libraryId") UUID libraryId);

    int countByDocIdAndVersion(@Param("docId") UUID docId, @Param("version") int version);

    void insertChunk(
            @Param("chunkId") UUID chunkId,
            @Param("libraryId") UUID libraryId,
            @Param("docId") UUID docId,
            @Param("tenantId") String tenantId,
            @Param("version") int version,
            @Param("chunkIndex") int chunkIndex,
            @Param("content") String content,
            @Param("metadataJson") String metadataJson,
            @Param("embedding") float[] embedding);

    List<SearchHit> search(
            @Param("libraryId") UUID libraryId,
            @Param("tenantId") String tenantId,
            @Param("embedding") float[] queryEmbedding,
            @Param("topK") int topK,
            @Param("docIds") List<UUID> docIds,
            @Param("metadataFilters") List<MetadataFilterClause> metadataFilters,
            @Param("chunkProfileIds") List<String> chunkProfileIds);

    int backfillChunkProfileMetadata(
            @Param("docId") UUID docId,
            @Param("version") int version,
            @Param("chunkProfileId") String chunkProfileId);

    List<SearchHit> keywordSearch(
            @Param("libraryId") UUID libraryId,
            @Param("tenantId") String tenantId,
            @Param("query") String query,
            @Param("topK") int topK,
            @Param("docIds") List<UUID> docIds,
            @Param("metadataFilters") List<MetadataFilterClause> metadataFilters,
            @Param("chunkProfileIds") List<String> chunkProfileIds);

    List<DocumentChunkRow> listByDocIdAndVersion(
            @Param("docId") UUID docId,
            @Param("version") int version);

    List<DocumentChunkRow> listByDocIdAndVersionPaged(
            @Param("docId") UUID docId,
            @Param("version") int version,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<DocChunkCountRow> countByDocVersions(@Param("pairs") List<DocVersionPair> pairs);

    List<String> findDistinctChunkProfileIds(
            @Param("libraryId") UUID libraryId, @Param("tenantId") String tenantId);

    int deleteOrphanChunksForProfile(
            @Param("libraryId") UUID libraryId,
            @Param("tenantId") String tenantId,
            @Param("chunkProfileId") String chunkProfileId);
}
