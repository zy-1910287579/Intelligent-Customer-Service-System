package com.storm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Slf4j
@Service
public class RagChatService {

    private final @Qualifier("ragChatClient") ChatClient ragChatClient;
    private final Advisor retrievalAugmentationAdvisor; // 注入上面定义的通用 Advisor


    public Flux<String> chat(String question, String userId, String sessionId) {
        // 1. 动态构建过滤表达式字符串
        // 注意：这里必须和你存入 Metadata 时的字段名完全一致
        // 语法参考：https://docs.spring.io/spring-ai/reference/api/vectorstores.html#_filtering
        // 假设你的 Metadata 存的是 String 类型，格式应该是 key == 'value'
        String filterExpression = String.format("user_id == '%s' AND session_id == '%s'", userId, sessionId);

        log.info("当前检索过滤器: {}", filterExpression);

        // 2. 调用 ChatClient，并注入动态参数
        return ragChatClient.prompt()
                .user(question)
                // 使用通用的 Advisor
                .advisors(retrievalAugmentationAdvisor)
                // 【核心】在这里动态注入过滤器！
                // 这会覆盖或补充 Advisor 里的检索配置
                //.advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression))
                .stream()
                .content();
    }
}
