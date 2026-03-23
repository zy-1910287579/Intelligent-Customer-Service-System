package com.storm.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// 就这一个类，够了！
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handle(Exception e) {
        log.error("请求失败", e); // ← 给你看的（控制台/日志）
        return ResponseEntity.status(500)
                .body(Map.of("error", "服务忙")); // ← 给前端看的
    }
}