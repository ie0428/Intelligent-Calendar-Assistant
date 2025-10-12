package com.ai.intelligentcalendarandconflictdetectionassistant.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 增强的对话记忆管理服务
 */
@Service
@AllArgsConstructor
public class EnhancedChatMemoryService {

    private final ChatMemoryStore chatMemoryStore;

    /**
     * 为会话创建ChatMemory实例
     */
    public ChatMemory createChatMemory(String sessionId) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(20) // 保留最近20条消息
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    /**
     * 获取会话的对话历史
     */
    public List<ChatMessage> getConversationHistory(String sessionId) {
        return chatMemoryStore.getMessages(sessionId);
    }

    /**
     * 清除会话的对话记忆
     */
    public void clearChatMemory(String sessionId) {
        chatMemoryStore.deleteMessages(sessionId);
    }

    /**
     * 获取对话记忆的摘要信息
     */
    public String getMemorySummary(String sessionId) {
        List<ChatMessage> messages = getConversationHistory(sessionId);
        
        if (messages.isEmpty()) {
            return "无对话历史";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("对话历史摘要（最近" + messages.size() + "条消息）：\n");
        
        for (int i = 0; i < Math.min(messages.size(), 5); i++) { // 最多显示5条
            ChatMessage message = messages.get(i);
            String role = message.type().toString().toLowerCase();
            String content = message.text();
            
            if (content.length() > 50) {
                content = content.substring(0, 47) + "...";
            }
            
            summary.append("[").append(role).append("]: ").append(content).append("\n");
        }
        
        return summary.toString();
    }

    /**
     * 检查会话是否有对话历史
     */
    public boolean hasConversationHistory(String sessionId) {
        return !getConversationHistory(sessionId).isEmpty();
    }

    /**
     * 获取对话记忆的统计信息
     */
    public String getMemoryStatistics(String sessionId) {
        List<ChatMessage> messages = getConversationHistory(sessionId);
        
        long userMessages = messages.stream()
                .filter(msg -> msg.type().toString().equalsIgnoreCase("USER"))
                .count();
        
        long aiMessages = messages.stream()
                .filter(msg -> msg.type().toString().equalsIgnoreCase("AI"))
                .count();
        
        return String.format("对话统计：用户消息 %d 条，AI回复 %d 条，总计 %d 条消息", 
                userMessages, aiMessages, messages.size());
    }
}