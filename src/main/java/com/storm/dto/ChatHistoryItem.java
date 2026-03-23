// package com.storm.dto 或你常用的 dto 包下
package com.storm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatHistoryItem {
    private String userMessage;
    private String aiMessage;
    private LocalDateTime timestamp; // 可用用户提问的时间
}