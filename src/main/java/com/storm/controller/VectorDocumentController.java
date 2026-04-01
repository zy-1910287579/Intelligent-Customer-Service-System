package com.storm.controller;
import com.storm.entity.Result;
import com.storm.ai.rag.service.VectorDocumentManagerService;
import com.storm.ai.rag.service.TransformDocumentToVectorService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
@Validated
@RequiredArgsConstructor
@Slf4j
@RequestMapping("store")
@RestController
public class VectorDocumentController {
    private final VectorDocumentManagerService vectorDocumentManagerService; // 注入服务
    private final TransformDocumentToVectorService transformDocumentToVectorService;

/**
 * 对于 deleteById 这个接口，前端需要传入一个 ID 列表。
 * 在 Web API 中，传递这种集合数据最常用、最标准的方式就是使用 JSON 格式作为请求体。*/
    @DeleteMapping("id")
    public Result<String> deleteById(
            @NotEmpty(message = "文档ID列表不能为空")
            @Size(min = 1, message = "至少传入1个文档ID")@RequestBody List<
                    @NotBlank(message = "文档ID不能为空字符串")String> documentIds){
        log.info("收到删除请求，待删除ID数量: {}", documentIds.size());
        vectorDocumentManagerService.removeDocumentsByIds(documentIds);
        log.info("成功删除 {} 个文档。", documentIds.size());

        // 4. 使用 Result.success() 返回统一格式的成功响应
        return Result.success("成功删除 " + documentIds.size() + " 个文档。", "删除成功");
    }

    /**
     * 根据用户 ID 和会话 ID 清空一个会话中的所有文档
     *
     * 前端请求示例:
     * URL: DELETE /store/session
     * Method: DELETE
     * Content-Type: application/json
     * Request Body: {"userId": "user_123", "sessionId": "session_xyz"}
     */
    //优点：
    //扩展性强：如果未来需要增加更多参数（例如 fileType、timestamp），
    // 只需要在 ClearSessionRequest 对象中添加新字段，Controller 方法签名不变。
    //结构化：参数被打包成一个对象，逻辑上更内聚，特别是在参数较多时。
    //据用户 ID 和会话 ID 清空一个会话中的所有文档
    @DeleteMapping("session")
    public Result<String> clearSession(
            @NotBlank(message = "用户ID不能为空")@RequestParam String userId,
            @NotBlank(message = "会话ID不能为空")@RequestParam String sessionId) {
        log.info("收到清空会话请求，userId: {}, sessionId: {}", userId, sessionId);


        vectorDocumentManagerService.removeDocumentsByUserAndSession(userId, sessionId);
        log.info("成功清空用户 [{}] 会话 [{}] 中的所有文档。", userId, sessionId);
        return Result.success("成功清空会话中的文档。", "清空成功");

    }

    /**
     * 检查指定文件是否已存在于数据库中
     *
     * 前端请求示例:
     * URL: GET /store/check-file-exists?userId=user_123&sessionId=session_xyz&fileName=manual.pdf
     * Method: GET
     */
    //检查指定文件是否已存在于数据库中
    @GetMapping("check-file-exists")
    public Result<Boolean> checkFileExists(
            @NotBlank(message = "用户ID不能为空")@RequestParam String userId,
            @NotBlank(message = "会话ID不能为空")@RequestParam String sessionId,
            @NotBlank(message = "文件名不能为空")@RequestParam String fileName) {

        log.info("收到文件存在性检查请求，userId: {}, sessionId: {}, fileName: {}", userId, sessionId, fileName);

        boolean exists = vectorDocumentManagerService.checkFileExists(userId, sessionId, fileName);
        return Result.success(exists, "查询成功");
    }

    /**
     * 查询指定会话下的所有文档ID列表
     *
     * 前端请求示例:
     * URL: GET /store/list-ids?userId=user_123&sessionId=session_xyz
     * Method: GET
     */
    //查询指定会话下的所有文档ID列表
    @GetMapping("list-ids")
    public Result<List<String>> listDocumentIds(
            @NotBlank(message = "用户ID不能为空")@RequestParam String userId,
            @NotBlank(message = "会话ID不能为空")@RequestParam String sessionId) {

        log.info("收到查询文档ID列表请求，userId: {}, sessionId: {}", userId, sessionId);


        List<String> documentIds = vectorDocumentManagerService.findDocumentIdsByUserAndSession(userId, sessionId);
        return Result.success(documentIds, "查询成功");

    }

    @PostMapping("upload")
    public ResponseEntity<String> uploadDocument(@NotBlank(message = "文件为空")@RequestParam("file") MultipartFile file,
                                                 @NotBlank(message = "用户ID不能为空")@RequestParam String userId,
                                                 @NotBlank(message = "会话ID不能为空")@RequestParam String sessionId) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        // 3. 生成唯一文件名,MultipartFile.getOriginalFilename() 方法返回的是客户端上传文件的完整原始文件名，其中包含文件后缀
        String originalFilename = file.getOriginalFilename();

        if(vectorDocumentManagerService.checkFileExists(userId,sessionId,originalFilename)){
            return  ResponseEntity.ok("知识库文档已经上传成功过了哦！在项目 uploads 目录下。📚✨");
        }

        // 1. 【修改点】定义当前目录下的文件夹路径
        // "./uploads" 表示在项目运行根目录下创建一个 uploads 文件夹
        Path uploadDir = Paths.get("./uploads");
            // 2. 【修改点】自动创建目录（如果不存在）
            // createDirectories 会检查目录是否存在，不存在则创建，包括父目录
        Files.createDirectories(uploadDir);

        String uniqueFileName = userId+"_"+sessionId+"_"+originalFilename;

        // 4. 拼接最终的文件完整路径
        Path filePath = uploadDir.resolve(uniqueFileName);


        // 5. 保存文件
        Files.write(filePath, file.getBytes());

        // 6. 调用服务将文件内容分块并存入向量库
        // 这里传入绝对路径，确保服务能准确找到文件
        transformDocumentToVectorService.ingestFileToVectorStore(filePath.toAbsolutePath().toString(),originalFilename,userId,sessionId);

        log.info("文件 {} 成功上传并保存到: {}", originalFilename, filePath.toAbsolutePath());
        return ResponseEntity.ok("知识库文档上传成功！文件已保存在项目 uploads 目录下。📚✨");

    }



}
