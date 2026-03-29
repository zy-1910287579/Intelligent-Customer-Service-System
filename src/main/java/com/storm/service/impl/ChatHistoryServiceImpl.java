// package com.storm.service;
package com.storm.service.impl;

import com.storm.dto.ChatHistoryItem;
import com.storm.mapper.OurChatHistoryMapper;
import com.storm.repository.MyChatHistoryRepository;
import com.storm.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final OurChatHistoryMapper ourChatHistoryMapper;



    private final MyChatHistoryRepository chatHistoryRepository;

    /**
     * 根据 conversationId 获取完整的对话历史（问答对列表）
     */
    public List<ChatHistoryItem> getChatHistory(String userId,String conversationId) {

        String uniqueId=userId+"_"+conversationId;
        // 1. 从数据库查询所有原始消息（按时间排序）
        List<MyChatHistoryRepository.RawMessage> rawMessages =
                chatHistoryRepository.findMessagesByConversationId(uniqueId);
        log.debug("开始加载对话历史，ID: {}", uniqueId); // ← debug：开发用

        log.debug("查到 {} 条原始消息", rawMessages.size());

        // 2. 组装成问答对
        List<ChatHistoryItem> history = new ArrayList<>();
        for (int i = 0; i < rawMessages.size(); i++) {
            MyChatHistoryRepository.RawMessage current = rawMessages.get(i);

            // 只处理用户消息作为一轮对话的起点
            if ("USER".equals(current.getType())) {
                ChatHistoryItem item = new ChatHistoryItem();
                item.setUserMessage(current.getContent());
                item.setTimestamp(current.getTimestamp());

                // 查看下一条是否是 AI 回复
                String aiContent = "[回复生成中...]";
                if (i + 1 < rawMessages.size()) {
                    MyChatHistoryRepository.RawMessage next = rawMessages.get(i + 1);
                    if ("ASSISTANT".equals(next.getType())) {
                        aiContent = next.getContent();
                        i++; // 跳过已配对的 AI 消息
                    }
                }
                item.setAiMessage(aiContent);
                history.add(item);
            }
            // 如果遇到孤立的 assistant 消息（比如系统消息），可选择忽略
        }
        log.info("成功返回 {} 条问答记录，ID: {}", history.size(), uniqueId); // ← info：业务完成
        return history;
    }


    //根据userId和sessionId删除聊天记录
    @Override
    public void deleteHistoryByConversationId(String userId,String conversationId) {

        String uniqueId=userId+"_"+conversationId;
        log.info("正在删除会话 [{}] 的历史记录。", uniqueId);
        int deletedCount = ourChatHistoryMapper.deleteByConversationId(uniqueId);
        log.info("成功删除 {} 条历史记录。", deletedCount);
    }
    @Override
    public List<ChatMemory> getHistoryByConversationId(String userId,String conversationId) {
        String uniqueId=userId+"_"+conversationId;
        log.debug("正在查询会话 [{}] 的历史记录。", uniqueId);
        return ourChatHistoryMapper.selectByConversationId(uniqueId);
    }

}