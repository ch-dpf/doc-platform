package com.knowbase.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.persistence.entity.AgentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMapper extends BaseMapper<AgentEntity> {
}
