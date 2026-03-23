package com.storm.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ChatClientConfiguration {

    //其实你可以这样理解,配置类的链式调用是在配初始化参数

    /*JdbcChatMemoryRepository 在存取消息时，底层使用了 Spring 的 ObjectMapper（Jackson），
    把 Message 对象（如 UserMessage, AiMessage）转成 JSON 字符串，存入数据库的 content 字段。
    对于简单消息（只有文本），content 就是原始字符串
    但对于复杂消息（带 metadata、多模态等），它会存成 JSON*/
    //这里根据yml文件里的pg数据库配置,会自动注入pg的实现,
    //JdbcChatMemoryRepository是非内存存储,必须要指定数据库
    private  final JdbcChatMemoryRepository jdbcChatMemoryRepository;
    //TODO 后期用常量类
    private final static int maxMessages=20;
    private final PgVectorStore myPgVectorStore;
    @Bean
    public ChatMemory chatMemory(){
        /**配ChatMemory的各种参数
         * 1.ChatMemory的具体实现类,都是builder工厂创建
         * 2.具体实现类的的自定义存储类
         * 3.最大滑动窗口大小
         * 4.返回创建好的ChatMemory对象
         * */
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
        log.debug("初始化 ChatMemory - 类型是: {}, 最大消息数: {}, 自定义存储厂库是: {}",
                chatMemory.getClass().getSimpleName(),
                maxMessages,
                jdbcChatMemoryRepository.getClass().getSimpleName());
        return chatMemory;
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,ChatMemory chatMemory){
        log.info("正在初始化 ChatClient...使用的 ChatMemory 类型是: {},初始化成功!", chatMemory.getClass().getName());
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个可爱活泼的ai助手")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
    /**ChatMode会根据配置文件自动注入*/
    @Bean
    public ChatClient ragChatClient(OpenAiChatModel openAiChatModel,ChatMemory chatMemory ){
        /**标准的 RAG 实现中，检索到的 chunks（上下文）通常 不会 被写入聊天历史！*/
        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .template("""
        你是一个专业、准确且乐于助人的AI知识助手。
        请根据以下参考资料回答用户的问题：
       
        - 如果参考资料中有明确答案，请用自己的话简洁、清晰地回答，不要直接复制长段落。
        - 如果参考资料中没有相关信息，请明确回答：“根据现有资料，我无法回答该问题。”，**不要编造、推测或引用外部知识**。
        - 回答应聚焦问题本身，避免冗长，一般不超过10句话。
        - 使用正式但友好的中文，不使用表情符号或拟人化语气。

        参考资料：
        {question_answer_context}

        问题：
        {query}
        """).build();

        /**配置 QuestionAnswerAdvisor（带自定义模板）
         * 1.创建问题回复增强器(基于增强检索功能增强),builder工程模式创建,并传入数据库
         * 2.构建搜索请求器,并自定义参数,此搜索请求器会自动将用户的问题去向量库里找相似片段
         * 3.
         * 4.
         * */
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(myPgVectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.3) // 相似度阈值
                        .topK(10)                  // 最多返回6个片段
                        .build())
                .promptTemplate(customPromptTemplate)
                .build();

        log.info("正在初始化 ChatClient... 使用的 ChatMemory 类型: {}, 启用 RAG 问答顾问",
                chatMemory.getClass().getName());

        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个可爱活泼的ai助手") // 仍保留 system prompt（可选）
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        qaAdvisor  // 👈 RAG 顾问放在这里
                )
                .build();
    }

}
