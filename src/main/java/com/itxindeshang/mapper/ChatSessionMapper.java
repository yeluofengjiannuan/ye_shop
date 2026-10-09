package com.itxindeshang.mapper;

import com.itxindeshang.pojo.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxindeshang.pojo.vo.ChatSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天会话列表表 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    List<ChatSessionVO> selectSessionList(@Param("userId") Long userId);
}
