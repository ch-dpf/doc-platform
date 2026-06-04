package com.docplatform.vector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docplatform.vector.domain.ProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessedEventMapper extends BaseMapper<ProcessedEvent> {
}
