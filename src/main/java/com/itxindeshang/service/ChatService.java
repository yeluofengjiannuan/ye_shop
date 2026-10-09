package com.itxindeshang.service;

import com.itxindeshang.pojo.entity.ChatMessage;
import com.itxindeshang.pojo.vo.ChatSessionVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService extends IService<ChatMessage> {

    /**
     * 发送并持久化消息
     */
    ChatMessage saveAndGetMessage(Long fromUserId, Long toUserId, String content, Integer msgType, Long productId);

    /**
     * 获取会话列表
     */
    List<ChatSessionVO> getSessionList(Long userId);

    /**
     * 分页获取历史消息
     */
    Page<ChatMessage> getChatHistory(Long userId, Long contactId, Integer page, Integer size);

    /**
     * 清除未读数
     */
    void clearUnread(Long userId, Long contactId);
}
