package com.storm.controller;
import com.storm.service.VectorDocumentService;
import com.storm.tools.testTimeTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
    private final  @Qualifier("ragChatClient") ChatClient ragChatClient;
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
            @RequestParam String conversationId) {

        log.info("流式提问 - conversationId: {}, prompt: {}", conversationId, prompt);

        return ragChatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(new SimpleLoggerAdvisor()) // 可选：打印完整 prompt 到日志
                .user(prompt)
                .stream()
                .content();
    }


    @PostMapping("upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        // 1. 创建临时目录（如果不存在）
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "rag-uploads");
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            log.error("创建临时目录失败", e);
            return ResponseEntity.status(500).body("服务器内部错误：无法创建临时目录");
        }

        // 2. 生成唯一文件名，防止冲突
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID() + extension;
        Path filePath = tempDir.resolve(uniqueFileName);

        try {
            // 3. 保存文件到临时路径
            Files.write(filePath, file.getBytes());

            // 4. 调用服务将文件内容分块并存入向量库
            vectorDocumentService.ingestFileToVectorStore(filePath.toString());

            // 5. 可选：上传成功后删除临时文件（或保留用于审计）
            // Files.deleteIfExists(filePath);

            log.info("文件 {} 成功上传并入库", originalFilename);
            return ResponseEntity.ok("知识库文档上传成功！猫娘已学习～ 📚✨");
        } catch (Exception e) {
            log.error("文件上传或向量化失败: {}", originalFilename, e);
            return ResponseEntity.status(500).body("上传失败：" + e.getMessage());
        }
        }
    }
