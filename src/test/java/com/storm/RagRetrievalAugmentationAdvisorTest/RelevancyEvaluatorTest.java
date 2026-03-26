package com.storm.RagRetrievalAugmentationAdvisorTest;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//JUnit 5 不支持测试类用构造器注入！!!!!

@SpringBootTest
public class RelevancyEvaluatorTest {
    //已配置好的rag对话客户端
    @Autowired
    private @Qualifier("ragChatClient") ChatClient ragChatClient;

    @Autowired
    private  ChatModel chatModel;
    @Autowired
    private  VectorStore myPgvectorStore; // 您项目中配置的 VectorStore

    @Autowired//哎呀,单独提取出来用就是舒服!
    private Advisor retrievalAugmentationAdvisor;


    @Test
    void testVectorStoreRetrievalAccuracy() {
        String question = "毕马威的全球调研显示了什么?";
        String expectedKeyContent = "中国职场的 AI 工具使用率高达 93%"; // 您期望被检索到的关键信息

        List<Document> retrievedDocuments = myPgvectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(2) // 根据需要调整
                        .build()
        );

        // 断言：检查检索结果中是否包含了我们期望的关键信息
        boolean containsExpectedContent = retrievedDocuments.stream()
                .anyMatch(doc -> doc.getText().contains(expectedKeyContent));

        assertThat(containsExpectedContent)
                .as("The retrieved documents should contain the key information: '%s'", expectedKeyContent)
                .isTrue();
    }


    //评估回答是否解决了用户的问题且与上下文一致。
    @Test
    void testRagResponseAccuracy(){
        // 1. 准备问题
        String question = "毕马威的全球调研显示了什么?";

        // 1. 构建 RAG 流程获取响应
        ChatResponse chatResponse = ChatClient.builder(chatModel).build()
                .prompt(question)
                .advisors(retrievalAugmentationAdvisor) // 使用 RAG Advisor
                .call()
                .chatResponse();

        // 2. 构建评估请求
        Assertions.assertNotNull(chatResponse);
        EvaluationRequest evaluationRequest = new EvaluationRequest(

                question, // 用户问题
                chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT), //被检索到的上下文
                chatResponse.getResult().getOutput().getText() // AI 单纯的回答,和.call().content()一样
        );

        // 3. 执行评估
        RelevancyEvaluator evaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));
        EvaluationResponse evaluationResponse = evaluator.evaluate(evaluationRequest);

        // 4. 验证结果
        assertThat(evaluationResponse.isPass()).isTrue();

    }
}
