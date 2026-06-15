package com.knowbase.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowbase.chat.domain.ChatConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {}
