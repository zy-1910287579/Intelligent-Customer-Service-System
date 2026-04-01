package com.storm.common.exception;

import com.storm.common.enums.ErrorCode;
import com.storm.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Map;

// 就这一个类，够了！
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 1. 处理业务异常 (BusinessException)
     * 场景：Service 层主动抛出的已知业务错误（如：余额不足、AI 调用失败）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK) // 业务异常通常返回 200，让前端根据 code 判断
    public Mono<Result<Void>> handleBusinessException(BusinessException e) {
        // 记录警告日志，不需要堆栈，只需要知道发生了什么业务阻断
        log.warn("⚠️ 业务异常：[{}] - {}", e.getCode(), e.getMessage());
        return Mono.just(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 2. 处理参数校验异常 (MethodArgumentNotValidException)
     * 场景：Controller 层 @RequestBody + @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Result<String>> handleValidationException(MethodArgumentNotValidException e) {
        // 提取第一个校验错误的字段信息
        String message = e.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");

        log.warn("⚠️ 参数校验失败：{}", message);
        return Mono.just(Result.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    // 2. 新增的：处理集合内部元素校验失败 (如 List<@NotBlank String>)
    // 或者 @RequestParam 校验失败
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Result<String>> handleConstraintViolationException(ConstraintViolationException e) {
        // 提取错误信息
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数格式不正确");

        log.warn("⚠️ 集合/参数校验失败：{}", message);
        return Mono.just(Result.error(400, message));
    }




    /**
     * 3. 处理参数绑定异常 (BindException)
     * 场景：Controller 层 @ModelAttribute 或表单提交校验失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Result<String>> handleBindException(BindException e) {
        String message = e.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("请求参数格式错误");

        log.warn("⚠️ 参数绑定失败：{}", message);
        return Mono.just(Result.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    /**
     * 4. 兜底：处理所有其他未知异常 (Exception)
     * 场景：空指针、数组越界、数据库连接超时等系统级错误
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Result<Void>> handleGlobalException(Exception e) {
        // ⚠️ 重点：生产环境严禁直接将 e.getMessage() 或 e.toString() 返回给前端，会泄露堆栈信息！
        // 这里只记录完整的堆栈信息供开发人员排查
        log.error("❌ 系统未知异常：", e);

        return Mono.just(Result.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }

}