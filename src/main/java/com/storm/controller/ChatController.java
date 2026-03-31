package com.storm.controller;
import com.storm.service.OrderService;
import com.storm.tools.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jaxb.core.v2.TODO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("ai")
@RestController
public class ChatController {

    //默认情况下，Spring AI 会自动配置单个豆子。 不过，你可能需要在申请中同时使用多个聊天模型。 以下是处理这种情况的方法：ChatClient.Builder
    //无论哪种情况，你都需要通过设置属性 来禁用自动配置。ChatClient.Builderspring.ai.chat.client.enabled=false
    //已配置好的对话客户端
    private final @Qualifier("chatClient") ChatClient  chatClient;

    //注入工具类
    private final UserAssistanceTools userAssistanceTools;
    private final OrderAssistanceTools orderAssistanceTools;
    private final TicketAssistanceTools ticketAssistanceTools;
    private final RagAssistanceTools ragAssistanceTools;

    private final ChatClient ragChatClient;


    @RequestMapping("talk")
    public String talk(@RequestParam(value = "prompt",defaultValue = "你好") String prompt){
        log.info("用户的提问是:{}",prompt);
        String s=chatClient
                //开始构建提示词
                .prompt()
                //放入用户消息
                .user(prompt)
                //发送请求并获取响应
                .call()
                // 从响应中提取文本内容
                .content();
                //如果这里是.chatResponse();那么返回值就是,ChatResponse chatResponse
        return s;
    }

    //produces = "text/plain;charset=UTF-8"可以防止前端乱码
    @RequestMapping(value = "chat",produces = "text/plain;charset=UTF-8")
    public Flux<String> chat(@RequestParam(value = "prompt",defaultValue = "你好") String prompt,
                             @RequestParam String userId,@RequestParam String sessionId){
        // TODO 上线前：替换为真实用户ID（如从JWT获取）
        log.info("用户的提问是:{}",prompt);
        //其实你可以这样理解,配置类的链式调用是在配初始化参数,而这里的就是在和ai对话了
        //所以配置列里的defaultAdvisors是全局的,只要你用了那个bean,就一直有那个配置
        return  chatClient.prompt()//1.开始构建提示词
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId+"_"+sessionId))
                    .advisors(new SimpleLoggerAdvisor())//此响应方法的增强配置
                .user(prompt)//开始放入用户问题
                .tools(orderAssistanceTools, userAssistanceTools, ticketAssistanceTools,ragAssistanceTools)
                .stream()//发送请求并获取响应
                .content();// 从响应中提取文本内容
    }
    /*
    TODO 后期可能会实现单窗口支持多文件问答功能,(@文件名+后端解析文件名并过滤参数)
     目前入库阶段:从普通tokens切分-->递归符号切分,
     出库检索功能:从普通QuestionAnswerAdvisor-->retrievalAugmentationAdvisor模块化优化检索流程
     上述改善已对rag检索回答功能已有很大改善
     进阶:1.可能语义分割会更好?
         2.实现普通对话和rag智能识别切换?
     */

    @RequestMapping("ragchat")
    public Flux<String> ragChat(
            @RequestParam(value = "prompt", defaultValue = "你好") String prompt,
            @RequestParam String userId,
            @RequestParam String sessionId) {


        String filterExpression = String.format("user_id == '%s' AND session_id == '%s'", userId, sessionId);

        log.info("当前检索过滤器: {}", filterExpression);

        log.info("流式提问 - userId: {}, prompt: {}", userId, prompt);

        return ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId+"_"+sessionId))
                .advisors(new SimpleLoggerAdvisor()) // 可选：打印完整 prompt 到日志
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression))
                .stream()
                .content();
    }

}
