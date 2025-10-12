package com.ai.intelligentcalendarandconflictdetectionassistant.request;

import lombok.Data;

@Data
public class CreateConversationRequest {
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String intent;
    private String entities;
    private Boolean successful;
}