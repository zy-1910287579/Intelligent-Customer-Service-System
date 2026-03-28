package com.storm.controller;
import com.storm.service.RagChatService;
import com.storm.service.VectorDocumentService;
import com.storm.tools.testTimeTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("ai")
@RestController
public class ChatController {

    //默认情况下，Spring AI 会自动配置单个豆子。 不过，你可能需要在申请中同时使用多个聊天模型。 以下是处理这种情况的方法：ChatClient.Builder
    //无论哪种情况，你都需要通过设置属性 来禁用自动配置。ChatClient.Builderspring.ai.chat.client.enabled=false
    //已配置好的对话客户端
    private final @Qualifier("chatClient") ChatClient  chatClient;
    //自己的服务层
    private final VectorDocumentService vectorDocumentService;
    //已配置好的rag对话客户端
    //private final RagChatService ragChatService;

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

    @RequestMapping("chat")
    public Flux<String> chat(@RequestParam(value = "prompt",defaultValue = "你好") String prompt,@RequestParam String conversationId){
        // TODO 上线前：替换为真实用户ID（如从JWT获取）
        log.info("用户的提问是:{}",prompt);
        //其实你可以这样理解,配置类的链式调用是在配初始化参数,而这里的就是在和ai对话了
        //所以配置列里的defaultAdvisors是全局的,只要你用了那个bean,就一直有那个配置
        Flux<String> content = chatClient.prompt()//1.开始构建提示词
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .advisors(new SimpleLoggerAdvisor())//此响应方法的增强配置
                .user(prompt)//开始放入用户问题
                .tools(new testTimeTools())
                .stream()//发送请求并获取响应
                .content();// 从响应中提取文本内容
        return content;
    }

    @RequestMapping("ragchat")
    public Flux<String> ragChat(
            @RequestParam(value = "prompt", defaultValue = "你好") String prompt,
            @RequestParam String conversationId,
            @RequestParam String sessionId) {


        String filterExpression = String.format("user_id == '%s' AND session_id == '%s'", conversationId, sessionId);

        log.info("当前检索过滤器: {}", filterExpression);

        log.info("流式提问 - conversationId: {}, prompt: {}", conversationId, prompt);

        return ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId+sessionId))
                .advisors(new SimpleLoggerAdvisor()) // 可选：打印完整 prompt 到日志
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExpression))
                .stream()
                .content();
    }

    @PostMapping("upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file,
     @RequestParam String conversationId, @RequestParam String sessionId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        // 1. 【修改点】定义当前目录下的文件夹路径
        // "./uploads" 表示在项目运行根目录下创建一个 uploads 文件夹
        Path uploadDir = Paths.get("./uploads");

        try {
            // 2. 【修改点】自动创建目录（如果不存在）
            // createDirectories 会检查目录是否存在，不存在则创建，包括父目录
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.error("创建目录失败", e);
            return ResponseEntity.status(500).body("服务器内部错误：无法创建存储目录");
        }

        // 3. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 保持 UUID 命名防止冲突
        String uniqueFileName = conversationId+originalFilename+ extension;

        // 4. 拼接最终的文件完整路径
        Path filePath = uploadDir.resolve(uniqueFileName);

        try {
            // 5. 保存文件
            Files.write(filePath, file.getBytes());

            // 6. 调用服务将文件内容分块并存入向量库
            // 这里传入绝对路径，确保服务能准确找到文件
            vectorDocumentService.ingestFileToVectorStore(filePath.toAbsolutePath().toString(),originalFilename,conversationId,sessionId);

            log.info("文件 {} 成功上传并保存到: {}", originalFilename, filePath.toAbsolutePath());
            return ResponseEntity.ok("知识库文档上传成功！文件已保存在项目 uploads 目录下。📚✨");

        } catch (Exception e) {
            log.error("文件上传或向量化失败: {}", originalFilename, e);
            return ResponseEntity.status(500).body("上传失败：" + e.getMessage());
        }
    }



}
