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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> runHandle(Exception e) {
        log.error("是运行时异常", e); // ← 给你看的（控制台/日志）
        return ResponseEntity.status(500)
                .body(Map.of("error", "是运行时异常")); // ← 给前端看的
    }

}