package com.itxindeshang.service;

import com.itxindeshang.pojo.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 聊天服务接口
 */
public interface ChatService extends IService<ChatMessage> {

    /**
     * 发送并持久化消息
     */
    ChatMessage saveAndGetMessage(Long fromUserId, Long toUserId, String content, Integer msgType, Long productId);


    /**
     * 清除未读数
     */
    void clearUnread(Long userId, Long contactId);
}
