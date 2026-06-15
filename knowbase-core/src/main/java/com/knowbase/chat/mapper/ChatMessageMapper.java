package com.knowbase.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.chat.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {}
