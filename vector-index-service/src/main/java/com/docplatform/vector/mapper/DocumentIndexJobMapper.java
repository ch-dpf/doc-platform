package com.docplatform.vector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docplatform.vector.domain.DocumentIndexJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface DocumentIndexJobMapper extends BaseMapper<DocumentIndexJob> {

    @Select("""
            SELECT * FROM vector_idx.document_index_job
            WHERE doc_id = #{docId}
              AND version = #{version}
            LIMIT 1
            """)
    DocumentIndexJob findByDocIdAndVersion(@Param("docId") UUID docId, @Param("version") int version);
}
