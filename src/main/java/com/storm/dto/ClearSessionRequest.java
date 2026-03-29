package com.storm.dto;

import lombok.Data;

@Data
public class ClearSessionRequest {
    private String userId;
    private String sessionId;
}