package com.ai.intelligentcalendarandconflictdetectionassistant.langchain4j;

import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.ConversationServiceImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.UserContextHolder;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * 增强的AI控制器，整合RAG、对话记忆管理和提示工程
 */
@RestController
@CrossOrigin
public class EnhancedAIController {

    private final ChatLanguageModel chatModel;
    private final LangChainBookingTools bookingTools;
    private final EnhancedRAGService enhancedRAGService;
    private final EnhancedChatMemoryService chatMemoryService;
    private final PromptEngineeringService promptService;
    private final ConversationServiceImpl conversationServiceImpl;

    public EnhancedAIController(ChatLanguageModel chatModel,
                               LangChainBookingTools bookingTools,
                               EnhancedRAGService enhancedRAGService,
                               EnhancedChatMemoryService chatMemoryService,
                               PromptEngineeringService promptService,
                               ConversationServiceImpl conversationServiceImpl) {
        this.chatModel = chatModel;
        this.bookingTools = bookingTools;
        this.enhancedRAGService = enhancedRAGService;
        this.chatMemoryService = chatMemoryService;
        this.promptService = promptService;
        this.conversationServiceImpl = conversationServiceImpl;
    }

    // 定义增强的AI服务接口
    interface LangChainEnhancedAssistant {
        String chat(@UserMessage String message, @MemoryId String sessionId);
    }

    /**
     * 增强的AI对话接口，整合所有高级功能
     */
    @CrossOrigin
    @GetMapping(value = "/ai/enhanced/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> enhancedGenerateStreamAsString(
            @RequestParam(value = "message", defaultValue = "讲个笑话") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "useRag", defaultValue = "true") boolean useRag,
            @RequestParam(value = "useMemory", defaultValue = "true") boolean useMemory) {

        // 获取当前用户ID
        final Long currentUserId = getCurrentUserId(userId);
        
        if (currentUserId == null) {
            return Flux.just("错误：无法识别用户身份，请重新登录")
                    .concatWith(Flux.just("[complete]"));
        }

        // 设置用户上下文，LangChainBookingTools会自动获取
        UserContextHolder.setCurrentUserId(currentUserId);

        // 生成或使用现有会话ID
        final String finalSessionId = (sessionId != null) ? sessionId : generateSessionId(currentUserId);
        
        System.out.println("增强AI对话 - 用户ID: " + currentUserId + ", 会话ID: " + finalSessionId);

        try {
            // 创建动态系统提示词
            String dynamicSystemPrompt = promptService.generateSmartSystemPrompt(finalSessionId, message);
            
            // 创建ChatMemory实例
            ChatMemory chatMemory = chatMemoryService.createChatMemory(finalSessionId);
            
            // 创建增强的AI服务实例
            LangChainEnhancedAssistant enhancedAssistant = AiServices.builder(LangChainEnhancedAssistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(bookingTools)
                    .chatMemory(chatMemory)
                    .systemMessageProvider((object) -> dynamicSystemPrompt)
                    .build();
            
            // 预处理用户消息
            String processedMessage = message;
            if (useRag) {
                processedMessage = promptService.generateRAGEnhancedMessage(finalSessionId, message);
            }
            
            // 调用AI服务
            String response = enhancedAssistant.chat(processedMessage, finalSessionId);
            
            // 记录对话统计
            logConversationStatistics(finalSessionId);
            
            // 返回响应流
            return Flux.just(response)
                    .doOnNext(r -> System.out.println("增强AI响应: " + r))
                    .concatWith(Flux.just("[complete]"))
                    .doFinally(signal -> {
                        // 清理ThreadLocal
                        UserContextHolder.clear();
                    });
                    
        } catch (Exception e) {
            System.err.println("增强AI对话发生错误: " + e.getMessage());
            e.printStackTrace();
            return Flux.just("抱歉，处理您的请求时发生了错误。")
                    .concatWith(Flux.just("[complete]"))
                    .doFinally(signal -> UserContextHolder.clear());
        }
    }

    /**
     * 获取对话记忆统计信息
     */
    @GetMapping("/ai/memory/statistics")
    public String getMemoryStatistics(@RequestParam("sessionId") String sessionId) {
        return chatMemoryService.getMemoryStatistics(sessionId);
    }

    /**
     * 清除对话记忆
     */
    @PostMapping("/ai/memory/clear")
    public String clearMemory(@RequestParam("sessionId") String sessionId) {
        chatMemoryService.clearChatMemory(sessionId);
        return "对话记忆已清除";
    }

    /**
     * 检查会话是否有对话历史
     */
    @GetMapping("/ai/memory/hasHistory")
    public boolean hasConversationHistory(@RequestParam("sessionId") String sessionId) {
        return chatMemoryService.hasConversationHistory(sessionId);
    }

    /**
     * 增强的文档嵌入接口
     */
    @PostMapping("/ai/rag/enhanced/embedding")
    public Boolean enhancedEmbedding(@RequestParam("file") MultipartFile file) {
        return enhancedRAGService.embeddingWithLangChain4j(file);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(Long userId) {
        if (userId != null) {
            return userId;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        
        return null;
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId(Long userId) {
        return "session_" + userId + "_" + System.currentTimeMillis();
    }

    /**
     * 记录对话统计
     */
    private void logConversationStatistics(String sessionId) {
        String statistics = chatMemoryService.getMemoryStatistics(sessionId);
        System.out.println("对话统计: " + statistics);
    }
}