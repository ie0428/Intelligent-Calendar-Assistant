package com.ai.intelligentcalendarandconflictdetectionassistant.advisor;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.UserContextHolder;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseChatMemoryAdvisor implements RequestResponseAdvisor {

    private final ConversationService conversationService;
    private final String memoryPromptTemplate;
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String USER_ID_KEY = "userId";
    private static final int MAX_HISTORY_COUNT = 10; // 最大历史记录数

    // 用于存储每个会话的AdvisedRequest对象
    private final Map<String, AdvisedRequest> requestCache = new ConcurrentHashMap<>();
    // 用于存储每个会话的AI响应片段
    private final Map<String, StringBuilder> responseCache = new ConcurrentHashMap<>();

    public DatabaseChatMemoryAdvisor(ConversationService conversationService) {
        this(conversationService, "\nUse the conversation memory from the MEMORY section to provide accurate answers.\n\n---------------------\nMEMORY:\n{memory}\n---------------------\n\n");
    }

    public DatabaseChatMemoryAdvisor(ConversationService conversationService, String memoryPromptTemplate) {
        this.conversationService = conversationService;
        this.memoryPromptTemplate = memoryPromptTemplate;
    }

    @Override
    public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
        System.out.println("DatabaseChatMemoryAdvisor.adviseResponse 被调用");

        try {
            // 从缓存中获取 AdvisedRequest 对象
            String sessionId = getSessionIdFromContext(context);
            AdvisedRequest advisedRequest = requestCache.get(sessionId);

            if (advisedRequest == null) {
                System.out.println("advisedRequest 为 null");
                return response;
            }

            // 获取用户消息和AI响应
            String userMessage = advisedRequest.userText();
            String aiResponse = response.getResult().getOutput().getContent();

            System.out.println("准备保存对话记录 - 用户消息: " + userMessage + ", AI响应: " + aiResponse);

            // 获取会话参数
            Map<String, Object> advisorParams = advisedRequest.advisorParams();
            Long userId = extractUserIdFromParams(advisorParams);
            
            // 如果无法从参数获取用户ID，则从认证上下文中获取当前登录用户的ID
            if (userId == null || userId <= 0) {
                userId = getCurrentUserIdFromContext(context);
            }

            System.out.println("保存对话记录使用的参数 - sessionId: " + sessionId + ", userId: " + userId);

            // 确保userId不为null
            if (userId == null) {
                userId = extractUserIdFromSessionId(sessionId);
                
                // 如果仍然无法获取，使用默认用户ID（当前登录用户ID为3）
                if (userId == null) {
                    userId = 3L;
                }
            }

            // 保存对话记录
            Conversation savedConversation = conversationService.saveConversation(
                    userId,
                    sessionId,
                    userMessage,
                    aiResponse,
                    "", // intent 可以从AI响应中提取或通过其他方式识别
                    "{}", // entities 可以是JSON格式的实体信息
                    true // successful 默认为true，可以根据实际处理结果调整
            );

            System.out.println("对话记录保存成功，ID: " + savedConversation.getId());

            // 清理缓存
            requestCache.remove(sessionId);

        } catch (Exception e) {
            // 记录异常但不影响主流程
            System.err.println("保存对话记录时出错: ");
            e.printStackTrace();
        }

        System.out.println("DatabaseChatMemoryAdvisor.adviseResponse 执行完成");
        return response;
    }

    @Override
    public Flux<ChatResponse> adviseResponse(Flux<ChatResponse> fluxResponse, Map<String, Object> context) {
        System.out.println("DatabaseChatMemoryAdvisor.adviseResponse(Flux) 被调用");

        return Flux.defer(() -> {
            final String sessionId = getSessionIdFromContext(context);
            
            // 确保sessionId不为null
            if (sessionId == null) {
                // 使用final变量确保在lambda表达式中使用的是final或实际上final的变量
                final String finalSessionId = UUID.randomUUID().toString();
                // 初始化响应缓存
                responseCache.put(finalSessionId, new StringBuilder());

                return fluxResponse
                        .doOnNext(response -> {
                            try {
                                // 累积AI响应片段
                                String aiResponseFragment = response.getResult().getOutput().getContent();
                                if (aiResponseFragment != null && !aiResponseFragment.isEmpty()) {
                                    responseCache.get(finalSessionId).append(aiResponseFragment);
                                }
                            } catch (Exception e) {
                                System.err.println("累积AI响应时出错: ");
                                e.printStackTrace();
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                // 流完成时保存完整的对话记录
                                StringBuilder responseBuilder = responseCache.get(finalSessionId);
                                if (responseBuilder != null && responseBuilder.length() > 0) {
                                    String completeAiResponse = responseBuilder.toString();

                                    // 从缓存中获取 AdvisedRequest 对象
                                    AdvisedRequest advisedRequest = requestCache.get(finalSessionId);

                                    if (advisedRequest != null) {
                                        // 获取用户消息
                                        String userMessage = advisedRequest.userText();

                                        System.out.println("准备保存完整对话记录 - 用户消息: " + userMessage +
                                                ", 完整AI响应: " + completeAiResponse);

                                        // 获取会话参数
                                        Map<String, Object> advisorParams = advisedRequest.advisorParams();
                                        Long userId = extractUserIdFromParams(advisorParams);
                                        
                                        // 如果无法从参数获取用户ID，则从认证上下文中获取当前登录用户的ID
                                        if (userId == null || userId <= 0) {
                                            userId = getCurrentUserIdFromContext(context);
                                        }

                                        System.out.println("保存完整对话记录使用的参数 - sessionId: " + finalSessionId +
                                                ", userId: " + userId);

                                        // 确保userId不为null
                                        if (userId == null) {
                                            userId = extractUserIdFromSessionId(sessionId);
                                            
                                            // 如果仍然无法获取，使用默认用户ID（当前登录用户ID为3）
                                            if (userId == null) {
                                                userId = 3L;
                                            }
                                        }

                                        // 保存完整的对话记录
                                        Conversation savedConversation = conversationService.saveConversation(
                                                userId,
                                                finalSessionId,
                                                userMessage,
                                                completeAiResponse,
                                                "", // intent 可以从AI响应中提取或通过其他方式识别
                                                "{}", // entities 可以是JSON格式的实体信息
                                                true // successful 默认为true，可以根据实际处理结果调整
                                        );

                                        System.out.println("完整对话记录保存成功，ID: " + savedConversation.getId());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("保存完整对话记录时出错: ");
                                e.printStackTrace();
                            } finally {
                                // 清理缓存
                                responseCache.remove(finalSessionId);
                                requestCache.remove(finalSessionId);
                                System.out.println("DatabaseChatMemoryAdvisor.adviseResponse(Flux) 执行完成");
                            }
                        })
                        .doOnError(error -> {
                            // 发生错误时清理缓存
                            responseCache.remove(finalSessionId);
                            requestCache.remove(finalSessionId);
                            System.err.println("DatabaseChatMemoryAdvisor.adviseResponse(Flux) 发生错误: " + error.getMessage());
                        });
            } else {
                // sessionId不为null的情况
                // 初始化响应缓存
                responseCache.put(sessionId, new StringBuilder());

                return fluxResponse
                        .doOnNext(response -> {
                            try {
                                // 累积AI响应片段
                                String aiResponseFragment = response.getResult().getOutput().getContent();
                                if (aiResponseFragment != null && !aiResponseFragment.isEmpty()) {
                                    responseCache.get(sessionId).append(aiResponseFragment);
                                }
                            } catch (Exception e) {
                                System.err.println("累积AI响应时出错: ");
                                e.printStackTrace();
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                // 流完成时保存完整的对话记录
                                StringBuilder responseBuilder = responseCache.get(sessionId);
                                if (responseBuilder != null && responseBuilder.length() > 0) {
                                    String completeAiResponse = responseBuilder.toString();

                                    // 从缓存中获取 AdvisedRequest 对象
                                    AdvisedRequest advisedRequest = requestCache.get(sessionId);

                                    if (advisedRequest != null) {
                                        // 获取用户消息
                                        String userMessage = advisedRequest.userText();

                                        System.out.println("准备保存完整对话记录 - 用户消息: " + userMessage +
                                                ", 完整AI响应: " + completeAiResponse);

                                        // 获取会话参数
                                        Map<String, Object> advisorParams = advisedRequest.advisorParams();
                                        Long userId = extractUserIdFromParams(advisorParams);
                                        
                                        // 如果无法从参数获取用户ID，则从认证上下文中获取当前登录用户的ID
                                        if (userId == null || userId <= 0) {
                                            userId = getCurrentUserIdFromContext(context);
                                        }

                                        System.out.println("保存完整对话记录使用的参数 - sessionId: " + sessionId +
                                                ", userId: " + userId);

                                        // 确保userId不为null
                                        if (userId == null) {
                                            userId = 1L; // fallback到默认用户ID
                                        }

                                        // 保存完整的对话记录
                                        Conversation savedConversation = conversationService.saveConversation(
                                                userId,
                                                sessionId,
                                                userMessage,
                                                completeAiResponse,
                                                "", // intent 可以从AI响应中提取或通过其他方式识别
                                                "{}", // entities 可以是JSON格式的实体信息
                                                true // successful 默认为true，可以根据实际处理结果调整
                                        );

                                        System.out.println("完整对话记录保存成功，ID: " + savedConversation.getId());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("保存完整对话记录时出错: ");
                                e.printStackTrace();
                            } finally {
                                // 清理缓存
                                responseCache.remove(sessionId);
                                requestCache.remove(sessionId);
                                System.out.println("DatabaseChatMemoryAdvisor.adviseResponse(Flux) 执行完成");
                            }
                        })
                        .doOnError(error -> {
                            // 发生错误时清理缓存
                            responseCache.remove(sessionId);
                            requestCache.remove(sessionId);
                            System.err.println("DatabaseChatMemoryAdvisor.adviseResponse(Flux) 发生错误: " + error.getMessage());
                        });
            }
        });
    }

    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
        try {
            // 获取会话参数
            Map<String, Object> advisorParams = request.advisorParams();
            String sessionId = extractSessionIdFromParams(advisorParams);
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            Long userId = extractUserIdFromParams(advisorParams);

            // 如果无法从参数获取用户ID，则从认证上下文中获取当前登录用户的ID
            if (userId == null || userId <= 0) {
                userId = getCurrentUserIdFromContext(context);
            }

            // 确保userId不为null
            if (userId == null) {
                userId = 3L; // fallback到默认用户ID（当前登录用户ID为3）
            }

            System.out.println("DatabaseChatMemoryAdvisor - sessionId: " + sessionId + ", userId: " + userId);

            // 将AdvisedRequest存储到缓存中
            requestCache.put(sessionId, request);

            // 从数据库获取历史对话记录
            List<Conversation> conversationHistory = conversationService.getConversationHistory(sessionId);

            System.out.println("从数据库获取到 " + conversationHistory.size() + " 条历史记录");

            // 如果getConversationHistory返回的记录过多，限制数量
            if (conversationHistory.size() > MAX_HISTORY_COUNT) {
                conversationHistory = conversationHistory.subList(0, MAX_HISTORY_COUNT);
            }

            // 构建内存格式的对话历史
            String memoryContent = buildMemoryContent(conversationHistory);

            System.out.println("构建的记忆内容: " + memoryContent);

            // 构造系统提示词，包含对话记忆
            String systemPromptWithMemory = request.systemText() +
                    memoryPromptTemplate.replace("{memory}", memoryContent);

            // 创建新的请求，包含内存增强的系统提示
            return AdvisedRequest.from(request)
                    .withSystemText(systemPromptWithMemory)
                    .build();

        } catch (Exception e) {
            // 出现异常时，返回原始请求
            e.printStackTrace();
            return request;
        }
    }

    /**
     * 从参数中提取用户ID
     * @param advisorParams 参数映射
     * @return 用户ID，如果无法提取则返回null
     */
    private Long extractUserIdFromParams(Map<String, Object> advisorParams) {
        if (advisorParams != null && advisorParams.containsKey(USER_ID_KEY)) {
            Object userIdObj = advisorParams.get(USER_ID_KEY);
            if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                try {
                    return Long.parseLong((String) userIdObj);
                } catch (NumberFormatException e) {
                    System.err.println("无法将userId字符串转换为Long: " + userIdObj);
                }
            }
        }
        return null;
    }

    /**
     * 从参数中提取会话ID
     * @param advisorParams 参数映射
     * @return 会话ID，如果无法提取则返回null
     */
    private String extractSessionIdFromParams(Map<String, Object> advisorParams) {
        if (advisorParams != null && advisorParams.containsKey(SESSION_ID_KEY)) {
            Object sessionIdObj = advisorParams.get(SESSION_ID_KEY);
            if (sessionIdObj instanceof String) {
                return (String) sessionIdObj;
            }
        }
        return null;
    }

    /**
     * 从认证上下文中获取当前登录用户的ID
     * @param context 请求上下文
     * @return 当前用户ID，如果无法获取则返回null
     */
    private Long getCurrentUserIdFromContext(Map<String, Object> context) {
        // 首先尝试从ThreadLocal获取用户ID（来自AI调用链）
        Long threadLocalUserId = UserContextHolder.getCurrentUserId();
        if (threadLocalUserId != null) {
            System.out.println("从ThreadLocal获取到用户ID: " + threadLocalUserId);
            return threadLocalUserId;
        }
        
        // 其次尝试从SecurityContext获取用户ID（来自HTTP请求）
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                return userDetails.getId();
            }
        } catch (Exception e) {
            System.err.println("获取当前用户ID失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从context中提取sessionId
     * @param context 上下文
     * @return sessionId
     */
    private String getSessionIdFromContext(Map<String, Object> context) {
        if (context == null) return null;

        // 尝试从不同位置获取sessionId
        Object sessionIdObj = context.get("sessionId");
        if (sessionIdObj instanceof String) {
            return (String) sessionIdObj;
        }

        // 尝试从request中获取
        AdvisedRequest request = (AdvisedRequest) context.get("request");
        if (request != null) {
            Map<String, Object> advisorParams = request.advisorParams();
            if (advisorParams != null) {
                Object sessionId = advisorParams.get(SESSION_ID_KEY);
                if (sessionId instanceof String) {
                    return (String) sessionId;
                }
            }
        }

        return null;
    }

    /**
     * 构建内存内容格式
     * @param conversationHistory 对话历史记录
     * @return 格式化的内存内容字符串
     */
    private String buildMemoryContent(List<Conversation> conversationHistory) {
        if (conversationHistory.isEmpty()) {
            return "No previous conversation history.";
        }

        StringBuilder memoryBuilder = new StringBuilder();
        // 按时间倒序排列，最新的在前面
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            Conversation conversation = conversationHistory.get(i);
            memoryBuilder.append("User: ").append(conversation.getUserMessage()).append("\n");
            memoryBuilder.append("Assistant: ").append(conversation.getAiResponse()).append("\n\n");
        }

        return memoryBuilder.toString().trim();
    }

    /**
     * 从sessionId字符串中提取用户ID
     * @param sessionId 会话ID字符串
     * @return 用户ID，如果无法提取则返回null
     */
    private Long extractUserIdFromSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        
        // sessionId格式: "user-{userId}-session-{timestamp}"
        // 例如: "user-3-session-1758795908586"
        try {
            if (sessionId.startsWith("user-")) {
                int firstDashIndex = sessionId.indexOf("-");
                int secondDashIndex = sessionId.indexOf("-", firstDashIndex + 1);
                
                if (secondDashIndex > firstDashIndex + 1) {
                    String userIdStr = sessionId.substring(firstDashIndex + 1, secondDashIndex);
                    return Long.parseLong(userIdStr);
                }
            }
        } catch (Exception e) {
            System.err.println("从sessionId提取用户ID失败: " + e.getMessage() + ", sessionId: " + sessionId);
        }
        
        return null;
    }
}