package com.knowbase.ingest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.ingest.domain.DocMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DocMetadataMapper extends BaseMapper<DocMetadata> {

    @Select("""
            SELECT * FROM doc_metadata
            WHERE library_id = #{libraryId}
              AND tenant_id = #{tenantId}
              AND checksum_sha256 = #{checksumSha256}
              AND deleted = false
            LIMIT 1
            """)
    DocMetadata findByLibraryTenantChecksum(
            @Param("libraryId") UUID libraryId,
            @Param("tenantId") String tenantId,
            @Param("checksumSha256") String checksumSha256);

    @Select("""
            SELECT * FROM doc_metadata
            WHERE doc_id = #{docId}
              AND deleted = false
            LIMIT 1
            """)
    DocMetadata findByDocIdAndDeletedFalse(@Param("docId") UUID docId);

    @Select("""
            SELECT COUNT(*) FROM doc_metadata
            WHERE library_id = #{libraryId}
              AND deleted = false
            """)
    int countActiveByLibraryId(@Param("libraryId") UUID libraryId);

    @Select("""
            SELECT COALESCE(SUM(size_bytes), 0) FROM doc_metadata
            WHERE library_id = #{libraryId}
              AND deleted = false
            """)
    long sumSizeBytesByLibraryId(@Param("libraryId") UUID libraryId);

    @Select("""
            SELECT COUNT(DISTINCT chunk_profile_id) FROM doc_metadata
            WHERE library_id = #{libraryId}
              AND deleted = false
              AND chunk_profile_id IS NOT NULL
              AND chunk_profile_id <> ''
            """)
    int countDistinctChunkProfiles(@Param("libraryId") UUID libraryId);

    @Select("""
            SELECT COUNT(*) > 0 FROM doc_metadata
            WHERE library_id = #{libraryId}
              AND deleted = false
              AND chunk_profile_id = #{chunkProfileId}
            LIMIT 1
            """)
    boolean existsChunkProfileId(
            @Param("libraryId") UUID libraryId, @Param("chunkProfileId") String chunkProfileId);

    @Select("""
            SELECT d.chunk_profile_id AS chunkProfileId,
                   COUNT(DISTINCT d.doc_id)::int AS docCount,
                   COUNT(c.chunk_id)::int AS chunkCount
            FROM doc_metadata d
            LEFT JOIN document_chunk c ON c.doc_id = d.doc_id AND c.library_id = d.library_id
            WHERE d.library_id = #{libraryId}
              AND d.deleted = false
              AND d.chunk_profile_id IS NOT NULL
              AND d.chunk_profile_id <> ''
            GROUP BY d.chunk_profile_id
            ORDER BY docCount DESC, d.chunk_profile_id ASC
            """)
    List<ChunkProfileStatsRow> listChunkProfileStats(@Param("libraryId") UUID libraryId);

    @Select("""
            SELECT * FROM doc_metadata
            WHERE library_id = #{libraryId}
              AND deleted = false
              AND (chunk_profile_id IS NULL OR chunk_profile_id = '')
            """)
    List<DocMetadata> findMissingChunkProfile(@Param("libraryId") UUID libraryId);

    record ChunkProfileStatsRow(String chunkProfileId, int docCount, int chunkCount) {}
}

