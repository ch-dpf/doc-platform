package com.docplatform.ingest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docplatform.ingest.domain.DocMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface DocMetadataMapper extends BaseMapper<DocMetadata> {

    @Select("""
            SELECT * FROM ingest.doc_metadata
            WHERE tenant_id = #{tenantId}
              AND checksum_sha256 = #{checksumSha256}
              AND deleted = false
            LIMIT 1
            """)
    DocMetadata findByTenantIdAndChecksumSha256AndDeletedFalse(
            @Param("tenantId") String tenantId,
            @Param("checksumSha256") String checksumSha256);

    @Select("""
            SELECT * FROM ingest.doc_metadata
            WHERE tenant_id = #{tenantId}
              AND source_url = #{sourceUrl}
              AND source_type = 'CRAWL'
              AND deleted = false
            LIMIT 1
            """)
    DocMetadata findByTenantIdAndSourceUrlAndDeletedFalse(
            @Param("tenantId") String tenantId, @Param("sourceUrl") String sourceUrl);

    @Select("""
            SELECT * FROM ingest.doc_metadata
            WHERE doc_id = #{docId}
              AND deleted = false
            LIMIT 1
            """)
    DocMetadata findByDocIdAndDeletedFalse(@Param("docId") UUID docId);
}
