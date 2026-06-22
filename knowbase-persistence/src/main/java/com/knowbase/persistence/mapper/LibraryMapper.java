package com.knowbase.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.persistence.entity.LibraryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LibraryMapper extends BaseMapper<LibraryEntity> {
}
