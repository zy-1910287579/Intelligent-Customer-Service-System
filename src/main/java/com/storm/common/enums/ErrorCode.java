package com.storm.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 * 规范建议：
 * 1. 200: 成功
 * 2. 4xx: 客户端错误 (参数错误、权限问题)
 * 3. 5xx: 服务端错误 (系统异常)
 * 4. 1xxxx: 业务自定义错误 (AI相关、工具调用、RAG等)
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ================= 通用状态 =================
    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 请求失败，参数可能有问题
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未授权/未登录
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问/权限不足
     */
    FORBIDDEN(403, "禁止访问，权限不足"),

    /**
     * 资源未找到
     */
    NOT_FOUND(404, "请求的资源不存在"),

    /**
     * 内部服务器错误
     */
    INTERNAL_ERROR(500, "系统内部错误，请联系管理员"),

    // ================= AI 与 RAG 业务错误 (10000 - 19999) =================

    /**
     * AI 服务通用错误
     */
    AI_SERVICE_ERROR(10001, "AI 服务响应异常"),

    /**
     * AI 连接超时
     */
    AI_CONNECTION_TIMEOUT(10002, "AI 服务连接超时，请重试"),

    /**
     * 提示词构建失败
     */
    AI_PROMPT_ERROR(10003, "AI 提示词构建失败"),

    /**
     * 工具调用失败
     */
    TOOL_CALLING_FAILED(10010, "工具调用执行失败"),

    /**
     * 知识库检索失败
     */
    RAG_RETRIEVAL_ERROR(10020, "知识库检索异常"),

    /**
     * 文档解析失败 (Tika相关)
     */
    DOCUMENT_PARSE_ERROR(10021, "文档解析失败，请检查文件格式"),

    // ================= 业务逻辑错误 (20000 - 29999) =================

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(20001, "用户不存在"),

    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND(20002, "订单不存在"),

    /**
     * 会话不存在
     */
    SESSION_NOT_FOUND(20003, "会话已过期或不存在");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;
}
