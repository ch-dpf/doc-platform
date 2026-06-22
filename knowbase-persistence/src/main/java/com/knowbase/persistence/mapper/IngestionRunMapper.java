package com.knowbase.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.persistence.entity.IngestionRunEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestionRunMapper extends BaseMapper<IngestionRunEntity> {
}
