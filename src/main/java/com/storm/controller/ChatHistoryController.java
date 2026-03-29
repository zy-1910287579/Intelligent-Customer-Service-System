package com.storm.controller;

import com.storm.entity.Result;
import com.storm.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RequestMapping("history")
@RestController
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    /**
     * 根据用户 ID 和会话 ID 清空对话历史
     *
     * 前端请求示例 (优化后):
     * URL: DELETE /store/history?userId=user_123&sessionId=session_xyz
     * Method: DELETE
     */
    @DeleteMapping("remove")
    public Result<String> clearChatHistory(
            @RequestParam String userId,
            @RequestParam String sessionId) { // 接收 userId 和 sessionId

        log.info("收到清空对话历史请求，userId: {},sessionId: {}",userId,sessionId );

        try {
            // 调用服务层方法
            chatHistoryService.deleteHistoryByConversationId(userId,sessionId);
            log.info("成功清空会话 userId: {},sessionId: {}",userId,sessionId );
            return Result.success("清空对话历史成功", "清空成功");
        } catch (Exception e) {
            log.error("清空对话历史时发生错误", e);
            return Result.error("清空失败: " + e.getMessage());
        }
    }


}
