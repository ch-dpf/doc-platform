package com.knowbase.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.library.domain.VectorLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VectorLibraryMapper extends BaseMapper<VectorLibrary> {

    @Select("""
            SELECT DISTINCT elem AS tag
            FROM vector_library,
            LATERAL jsonb_array_elements_text((config_json::jsonb)->'tags') AS elem
            WHERE tenant_id = #{tenantId}
            ORDER BY elem
            """)
    List<String> selectDistinctTags(@Param("tenantId") String tenantId);
}
