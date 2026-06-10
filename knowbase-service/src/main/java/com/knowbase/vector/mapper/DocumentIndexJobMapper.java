package com.knowbase.vector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.vector.domain.DocumentIndexJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.UUID;

@Mapper
public interface DocumentIndexJobMapper extends BaseMapper<DocumentIndexJob> {

    @Select("""
            SELECT * FROM document_index_job
            WHERE doc_id = #{docId}
              AND version = #{version}
            LIMIT 1
            """)
    DocumentIndexJob findByDocIdAndVersion(@Param("docId") UUID docId, @Param("version") int version);
}
