package com.storm.config;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ChatClientConfiguration {

    //其实你可以这样理解,配置类的链式调用是在配初始化参数

    private final Advisor retrievalAugmentationAdvisor;

    private final Advisor questionAnswerAdvisor;

    @Bean
    public ChatClient chatClient(DashScopeChatModel DashScopeChatModel, ChatMemory chatMemory){
        ChatClient chatClient = ChatClient.builder(DashScopeChatModel)
                //.defaultSystem("你是一个可爱活泼的ai助手")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
        log.info("普通对话客户端初始化成功!,使用模型为:{}",DashScopeChatModel.getClass().getName());
        return chatClient;
    }
    @Bean
    public ChatClient ragChatClient(DashScopeChatModel DashScopeChatModel,ChatMemory chatMemory ){
        ChatClient ragChatClient = ChatClient.builder(DashScopeChatModel)
                /**.defaultSystem("你是一个可爱活泼的ai助手")rag顾问里有提示词模板,
                所以不要加默认系统提示词了不然有可能会覆盖*/
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        //想用哪种
                        retrievalAugmentationAdvisor  // 👈 RAG 顾问放在这里
                )
                .build();
        log.info("rag对话客户端初始化成功!,使用模型为:{}",DashScopeChatModel.getClass().getName());
        return ragChatClient;
    }

}
