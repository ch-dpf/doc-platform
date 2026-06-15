package com.knowbase.vector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.vector.domain.ProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapper<ProcessedEvent> {
}
