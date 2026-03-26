package com.storm.RagRetrievalAugmentationAdvisorTest;

/*测试AI应用需要评估生成内容，以确保AI模型没有产生幻觉反应。*/
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertFalse;
//JUnit 5 不支持测试类用构造器注入！!!!!



@SpringBootTest
public class FactCheckingEvaluatorTest {
    //已配置好的rag对话客户端
    @Autowired
    private   @Qualifier("ragChatClient") ChatClient ragChatClient;

    @Autowired
    private  ChatModel chatModel;
    @Autowired//哎呀,单独提取出来用就是舒服!
    private Advisor retrievalAugmentationAdvisor;

    //评估回答中的事实陈述（主张）是否被上下文逻辑支持。
    @Test
    void testRagResponseAccuracy(){

        String context="";

        // 1. 构建 RAG 流程获取响应
        String claim = ChatClient.builder(chatModel).build()
                .prompt(context)
                .advisors(retrievalAugmentationAdvisor) // 使用 RAG Advisor
                .call()
                .content();

        // 2. 创建评估器
        /**你的代码报错是完全正确的，
         * 因为 protected 构造方法限制了外部类的直接访问。
         * 官方文档的示例之所以能运行，是因为它们很可能在同一个包内。
         * 要解决你的问题，最简单且符合设计模式的方法是创建一个继承自 FactCheckingEvaluator 的子类，
         * 并在子类中调用父类的构造方法。*/
        var factCheckingEvaluator = FactCheckingEvaluator.builder(ChatClient.builder(chatModel)).build();

        // 3. 构建请求 (Context 作为 userText, Claim 作为 responseContent)
        EvaluationRequest evaluationRequest = new EvaluationRequest(context, Collections.emptyList(), claim);

// 4. 执行评估
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

// 5. 验证结果 (期望不通过，因为 Claim 与 Context 不符)
        assertFalse(evaluationResponse.isPass(), "The claim should not be supported by the context");

    }


}
