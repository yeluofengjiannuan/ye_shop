package com.itxindeshang.controller.user;

import com.itxindeshang.common.result.PageResult;
import com.itxindeshang.common.result.Result;
import com.itxindeshang.context.BaseContext;
import com.itxindeshang.pojo.entity.ChatMessage;
import com.itxindeshang.pojo.vo.ChatSessionVO;
import com.itxindeshang.service.ChatService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实时聊天相关接口
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "聊天接口", description = "实时聊天、会话列表相关接口")
public class ChatController {

    private final ChatService chatService;


    @PostMapping("/clearUnread/{contactId}")
    @Operation(summary = "手动清除某会话的未读数")
    public Result<Void> clearUnread(@PathVariable Long contactId) {
        Long userId = Long.valueOf(BaseContext.getUserId());
        chatService.clearUnread(userId, contactId);
        return Result.success();
    }
}
