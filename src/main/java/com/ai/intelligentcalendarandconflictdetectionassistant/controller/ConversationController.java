package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.SessionSummary;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

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

    @GetMapping("/user/current")
    public List<Conversation> getCurrentUserConversations() {
        Long userId = getCurrentUserId();
        return conversationService.getConversationHistoryByUser(userId);
    }

    @GetMapping("/user/{userId}")
    public List<Conversation> getConversationsByUser(@PathVariable Long userId) {
        // 验证当前用户是否有权限访问该用户的数据
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new SecurityException("无权访问其他用户的对话记录");
        }
        return conversationService.getConversationHistoryByUser(userId);
    }

    @GetMapping("/session/{sessionId}")
    public List<Conversation> getConversationsBySession(@PathVariable String sessionId) {
        return conversationService.getConversationHistory(sessionId);
    }

    @DeleteMapping("/{id}")
    public void deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
    }
    
    /**
     * 获取当前用户的所有会话ID列表
     * @return 会话ID列表
     */
    @GetMapping("/sessions/current")
    public List<String> getCurrentUserSessions() {
        Long userId = getCurrentUserId();
        return conversationService.getAllSessionIdsByUser(userId);
    }

    /**
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    @GetMapping("/sessions/user/{userId}")
    public List<String> getAllSessionsByUser(@PathVariable Long userId) {
        // 验证当前用户是否有权限访问该用户的数据
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new SecurityException("无权访问其他用户的会话列表");
        }
        return conversationService.getAllSessionIdsByUser(userId);
    }
    
    /**
     * 获取当前用户的会话总结列表
     * @return 会话总结列表
     */
    @GetMapping("/sessions/summaries/current")
    public List<SessionSummary> getCurrentUserSessionSummaries() {
        Long userId = getCurrentUserId();
        return conversationService.getSessionSummariesByUser(userId);
    }
    
    /**
     * 获取指定用户的会话总结列表
     * @param userId 用户ID
     * @return 会话总结列表
     */
    @GetMapping("/sessions/summaries/user/{userId}")
    public List<SessionSummary> getSessionSummariesByUser(@PathVariable Long userId) {
        // 验证当前用户是否有权限访问该用户的数据
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new SecurityException("无权访问其他用户的会话总结");
        }
        return conversationService.getSessionSummariesByUser(userId);
    }
    
    /**
     * 创建新对话
     * @param sessionId 自定义会话ID（可选）
     * @return 新创建的会话ID
     */
    @PostMapping("/new")
    public String createNewConversation(@RequestParam(required = false) String sessionId) {
        Long userId = getCurrentUserId();
        
        // 使用Service方法创建新对话
        String newSessionId = conversationService.createNewConversation(userId, sessionId);
        
        System.out.println("创建新对话成功 - 用户ID: " + userId + ", 会话ID: " + newSessionId);
        
        return newSessionId;
    }
    
    /**
     * 获取用户最近的活动会话ID
     * @return 最近的活动会话ID
     */
    @GetMapping("/recent-session")
    public String getRecentActiveSession() {
        Long userId = getCurrentUserId();
        String recentSessionId = conversationService.getRecentActiveSessionId(userId);
        
        if (recentSessionId == null) {
            // 如果没有最近会话，创建一个新的
            recentSessionId = conversationService.createNewConversation(userId, null);
        }
        
        return recentSessionId;
    }
    
    /**
     * 检查会话是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    @GetMapping("/session/{sessionId}/exists")
    public boolean checkSessionExists(@PathVariable String sessionId) {
        return conversationService.sessionExists(sessionId);
    }
    
    /**
     * 获取会话中的对话数量
     * @param sessionId 会话ID
     * @return 对话数量
     */
    @GetMapping("/session/{sessionId}/count")
    public int getConversationCount(@PathVariable String sessionId) {
        return conversationService.getConversationCount(sessionId);
    }
}
