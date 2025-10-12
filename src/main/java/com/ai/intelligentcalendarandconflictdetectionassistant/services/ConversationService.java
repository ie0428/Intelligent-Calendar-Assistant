package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.SessionSummary;

import java.util.List;

/**
 * 对话服务接口
 */
public interface ConversationService {
    
    /**
     * 保存对话记录到数据库
     * @param userId 用户ID（可选，如果为null则从SecurityContext获取）
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param aiResponse AI响应
     * @param intent 识别的意图
     * @param entities 提取的实体
     * @param successful 是否处理成功
     * @return 保存的对话记录
     */
    Conversation saveConversation(Long userId, String sessionId, String userMessage, String aiResponse,
                                   String intent, String entities, Boolean successful);
    
    /**
     * 根据ID查找对话记录
     * @param id 对话ID
     * @return 对话记录
     */
    Conversation findById(Long id);
    
    /**
     * 根据会话ID获取对话历史
     * @param sessionId 会话ID
     * @return 对话历史列表
     */
    List<Conversation> getConversationHistory(String sessionId);
    
    /**
     * 根据用户ID获取对话历史
     * @param userId 用户ID
     * @return 对话历史列表
     */
    List<Conversation> getConversationHistoryByUser(Long userId);
    
    /**
     * 删除对话记录
     * @param id 对话ID
     */
    void deleteConversation(Long id);
    
    /**
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    List<String> getAllSessionIdsByUser(Long userId);
    
    /**
     * 获取用户的会话总结列表
     * @param userId 用户ID
     * @return 会话总结列表
     */
    List<SessionSummary> getSessionSummariesByUser(Long userId);
    
    /**
     * 创建新对话会话
     * @param userId 用户ID
     * @param customSessionId 自定义会话ID（可选）
     * @return 新创建的会话ID
     */
    String createNewConversation(Long userId, String customSessionId);
    
    /**
     * 获取用户最近的活动会话ID
     * @param userId 用户ID
     * @return 最近的活动会话ID，如果没有则返回null
     */
    String getRecentActiveSessionId(Long userId);
    
    /**
     * 检查会话是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    boolean sessionExists(String sessionId);
    
    /**
     * 获取会话中的对话数量
     * @param sessionId 会话ID
     * @return 对话数量
     */
    int getConversationCount(String sessionId);
}