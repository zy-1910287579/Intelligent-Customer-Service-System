// 文件：ChatHistoryController.java
package com.storm.controller;

import com.storm.dto.ChatHistoryItem;
import com.storm.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    /**
     * 获取指定会话的完整对话历史（问答对列表）
     *
     * @param conversationId 会话ID（必填）
     * @return List<ChatHistoryItem>
     */
    @GetMapping("/history")
    public List<ChatHistoryItem> getChatHistory(@RequestParam String conversationId) {
        return chatHistoryService.getChatHistory(conversationId);
    }
}