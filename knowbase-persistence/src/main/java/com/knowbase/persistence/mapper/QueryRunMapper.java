package com.knowbase.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.persistence.entity.QueryRunEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QueryRunMapper extends BaseMapper<QueryRunEntity> {
}
