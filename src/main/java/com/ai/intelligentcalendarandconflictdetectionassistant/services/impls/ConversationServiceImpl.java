package com.ai.intelligentcalendarandconflictdetectionassistant.services.impls;

import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.ConversationMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.SessionSummary;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private UserServiceImpl userService;

    /**
     * 获取当前登录用户的ID
     * @return 当前用户ID
     * @throws SecurityException 如果用户未认证
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new SecurityException("用户未认证");
    }
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
    public Conversation saveConversation(Long userId, String sessionId, String userMessage, String aiResponse,
                                         String intent, String entities, Boolean successful) {
        // 如果userId为null，从SecurityContext获取当前登录用户的ID
        if (userId == null) {
            try {
                userId = getCurrentUserId();
                System.out.println("从SecurityContext获取到用户ID: " + userId);
            } catch (SecurityException e) {
                // 如果无法从SecurityContext获取，尝试从sessionId中提取用户ID
                if (sessionId != null && sessionId.startsWith("user-")) {
                    try {
                        // 从sessionId中提取用户ID，格式为 "user-{userId}-session-{timestamp}"
                        String[] parts = sessionId.split("-");
                        if (parts.length >= 2) {
                            userId = Long.parseLong(parts[1]);
                            System.out.println("从sessionId中提取到用户ID: " + userId);
                        }
                    } catch (NumberFormatException ex) {
                        System.err.println("无法从sessionId中提取用户ID: " + sessionId);
                    }
                }
            }
        }

        // 如果仍然无法确定用户ID，则使用默认用户
        if (userId == null) {
            // 使用默认用户ID 1，避免创建新用户
            userId = 1L;
            System.out.println("使用默认用户ID: " + userId);
        }

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setUserMessage(userMessage);
        conversation.setAiResponse(aiResponse);
        conversation.setIntent(intent);
        conversation.setEntities(entities);
        conversation.setSuccessful(successful);
        conversation.setCreatedAt(LocalDateTime.now());

        System.out.println("准备插入对话记录: userId=" + userId + ", sessionId=" + sessionId +
                ", userMessage=" + userMessage);

        conversationMapper.insert(conversation);

        System.out.println("对话记录插入完成，生成的ID: " + conversation.getId());
        return conversation;
    }

    /**
     * 根据ID查找对话记录
     * @param id 对话ID
     * @return 对话记录
     */
    public Conversation findById(Long id) {
        return conversationMapper.findById(id);
    }

    /**
     * 根据会话ID获取对话历史
     * @param sessionId 会话ID
     * @return 对话历史列表
     */
    public List<Conversation> getConversationHistory(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return new ArrayList<>();
        }
        List<Conversation> history = conversationMapper.findBySessionIdOrderByCreatedAtDesc(sessionId);
        System.out.println("获取到 " + (history != null ? history.size() : 0) + " 条历史记录，sessionId: " + sessionId);
        return history != null ? history : new ArrayList<>();
    }

    /**
     * 根据用户ID获取对话历史
     * @param userId 用户ID
     * @return 对话历史列表
     */
    public List<Conversation> getConversationHistoryByUser(Long userId) {
        return conversationMapper.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除对话记录
     * @param id 对话ID
     */
    public void deleteConversation(Long id) {
        conversationMapper.deleteById(id);
    }

    /**
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    public List<String> getAllSessionIdsByUser(Long userId) {
        return conversationMapper.findDistinctSessionIdsByUserId(userId);
    }

    /**
     * 获取用户的会话总结列表
     * @param userId 用户ID
     * @return 会话总结列表
     */
    public List<SessionSummary> getSessionSummariesByUser(Long userId) {
        List<String> sessionIds = conversationMapper.findDistinctSessionIdsByUserId(userId);
        List<SessionSummary> summaries = new ArrayList<>();

        for (String sessionId : sessionIds) {
            List<Conversation> conversations = conversationMapper.findBySessionIdOrderByCreatedAtDesc(sessionId);
            if (conversations != null && !conversations.isEmpty()) {
                SessionSummary summary = new SessionSummary();
                summary.setSessionId(sessionId);
                summary.setConversationCount(conversations.size());

                // 获取最后活动时间
                Conversation lastConversation = conversations.get(0);
                summary.setLastActivityTime(lastConversation.getCreatedAt().toString());

                // 生成会话总结
                String summaryText = generateSessionSummary(conversations);
                summary.setSummary(summaryText);

                summaries.add(summary);
            }
        }

        return summaries;
    }

    /**
     * 生成会话总结
     * @param conversations 会话中的对话列表
     * @return 会话总结
     */
    private String generateSessionSummary(List<Conversation> conversations) {
        if (conversations == null || conversations.isEmpty()) {
            return "无对话内容";
        }

        // 获取第一条和最后一条对话
        Conversation firstConversation = conversations.get(conversations.size() - 1);
        Conversation lastConversation = conversations.get(0);

        StringBuilder summary = new StringBuilder();
        summary.append("包含 ").append(conversations.size()).append(" 条对话");

        if (lastConversation.getIntent() != null && !lastConversation.getIntent().isEmpty()) {
            summary.append("，主要讨论：").append(lastConversation.getIntent());
        }

        // 如果第一条对话有内容，添加到总结中
        if (firstConversation.getUserMessage() != null && !firstConversation.getUserMessage().isEmpty()) {
            String firstMessage = firstConversation.getUserMessage();
            if (firstMessage.length() > 20) {
                firstMessage = firstMessage.substring(0, 20) + "...";
            }
            summary.append("，开始于：").append(firstMessage);
        }

        return summary.toString();
    }

    /**
     * 创建新对话会话
     * @param userId 用户ID
     * @param customSessionId 自定义会话ID（可选）
     * @return 新创建的会话ID
     */
    public String createNewConversation(Long userId, String customSessionId) {
        // 验证用户ID
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }

        // 生成会话ID
        String sessionId;
        if (customSessionId != null && !customSessionId.trim().isEmpty()) {
            sessionId = customSessionId;
        } else {
            sessionId = "user-" + userId + "-session-" + System.currentTimeMillis();
        }

        // 创建欢迎消息作为新对话的开始
        String welcomeMessage = "欢迎使用智能日程助手！请问有什么可以帮您的？";
        
        // 保存欢迎消息到数据库
        Conversation welcomeConversation = new Conversation();
        welcomeConversation.setUserId(userId);
        welcomeConversation.setSessionId(sessionId);
        welcomeConversation.setUserMessage("开始新对话");
        welcomeConversation.setAiResponse(welcomeMessage);
        welcomeConversation.setIntent("welcome");
        welcomeConversation.setEntities("{}");
        welcomeConversation.setSuccessful(true);
        welcomeConversation.setCreatedAt(LocalDateTime.now());

        conversationMapper.insert(welcomeConversation);

        System.out.println("创建新对话会话 - 用户ID: " + userId + ", 会话ID: " + sessionId);
        
        return sessionId;
    }

    /**
     * 获取用户最近的活动会话ID
     * @param userId 用户ID
     * @return 最近的活动会话ID，如果没有则返回null
     */
    public String getRecentActiveSessionId(Long userId) {
        return conversationMapper.findLatestSessionIdByUserId(userId);
    }

    /**
     * 检查会话是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        return conversationMapper.existsBySessionId(sessionId);
    }

    /**
     * 获取会话中的对话数量
     * @param sessionId 会话ID
     * @return 对话数量
     */
    public int getConversationCount(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return 0;
        }
        
        return conversationMapper.countBySessionId(sessionId);
    }

}
