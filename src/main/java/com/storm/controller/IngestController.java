package com.storm.controller;

import com.storm.service.VectorDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
public class IngestController {

    private final VectorDocumentService vectorDocumentService;

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件为空");
        }

        try {
            // 1. 保存上传的文件到临时目录
            Path tempDir = Files.createTempDirectory("ingest-");
            Path tempFile = tempDir.resolve(file.getOriginalFilename());
            file.transferTo(tempFile);

            // 2. 调用服务入库
            vectorDocumentService.ingestFileToVectorStore(tempFile.toString());

            // 3. （可选）删除临时文件
            // Files.delete(tempFile);
            // Files.delete(tempDir);

            return ResponseEntity.ok("文档已成功入库: " + file.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("入库失败: " + e.getMessage());
        }
    }
}