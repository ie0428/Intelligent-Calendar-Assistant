package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.Data;
@Data
public class SessionSummary {
    private String sessionId;
    private String summary; // 会话内容总结
    private String lastActivityTime; // 最后活动时间
    private int conversationCount; // 对话数量
    
    public SessionSummary() {}
    
    public SessionSummary(String sessionId, String summary, String lastActivityTime, int conversationCount) {
        this.sessionId = sessionId;
        this.summary = summary;
        this.lastActivityTime = lastActivityTime;
        this.conversationCount = conversationCount;
    }
}