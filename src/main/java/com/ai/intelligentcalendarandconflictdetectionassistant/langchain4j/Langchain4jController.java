package com.ai.intelligentcalendarandconflictdetectionassistant.langchain4j;

import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.ConversationServiceImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.RagServiceImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.UserContextHolder;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


/**
 * @author xushu
 * @version 1.0
 * @description:
 */
@RestController
@CrossOrigin
public class Langchain4jController {
    private final ChatLanguageModel chatModel;
    private final LangChainBookingTools bookingTools;
    private final RagServiceImpl ragService;
    private final VectorStore vectorStore;
    private final ConversationServiceImpl conversationServiceImpl;

    // 配置LangChain4j AI服务
    public Langchain4jController(ChatLanguageModel chatModel,
                                 LangChainBookingTools bookingTools,
                                 RagServiceImpl ragService,
                                 VectorStore vectorStore,
                                 ConversationServiceImpl conversationServiceImpl) {
        this.chatModel = chatModel;
        this.bookingTools = bookingTools;
        this.ragService = ragService;
        this.vectorStore = vectorStore;
        this.conversationServiceImpl = conversationServiceImpl;
    }

    // 定义AI服务接口 - 使用LangChain4j的现代特性
    interface Assistant {
        @SystemMessage("""
                您是"ie"智能日程管理助手的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                您正在通过在线聊天系统与客户互动。
                
                重要提示：
                1. 每个日程都有一个唯一的数字ID，这是在创建日程时系统自动生成的（存储在数据库表中）
                2. 在执行取消或修改日程操作时，必须使用这个数字ID，而不是日程的状态或其他信息
                3. 当用户想要修改或取消日程时，请先使用getBookings工具获取当前用户的所有日程
                4. 然后根据用户描述匹配相应的日程，获取其ID
                5. 最后使用获取到的ID执行相应的修改或取消操作
                6. 用户不需要提供姓名，系统会自动使用当前登录用户的身份信息
                7. 当用户询问日程时，直接告诉用户他的日程信息，不需要再询问姓名
                8. 所有日程操作现在都基于用户ID进行，确保数据安全性和准确性
                9. 当用户要求删除日程时，这将完全从数据库中移除记录，而不仅仅是更改状态
                10. 取消操作会保留日程记录但将其状态设置为已取消，而删除操作会完全移除日程记录
                11. 您可以使用以下工具来帮助用户：
                   - createBooking: 创建新的日程安排
                   - getBookings: 查询用户日程
                   - cancelBooking: 取消日程
                   - changeBooking: 修改日程
                   - checkConflict: 检测日程冲突
                
                请讲中文。
                """)
        String chat(@UserMessage String message, @MemoryId String memoryId);
    }
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsStringlangchain4j", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "讲个笑话") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "useRag", defaultValue = "false") boolean useRag) {

        // 优先使用URL参数中的userId，如果没有则尝试从认证中获取
        final Long currentUserId;
        if (userId != null) {
            currentUserId = userId;
        } else {
            currentUserId = getCurrentUserId();
        }
        
        System.out.println("LangChain4j接口调用 - 用户ID: " + currentUserId + ", 会话ID: " + sessionId + ", 消息: " + message);

        // 如果无法获取用户ID，返回错误信息
        if (currentUserId == null) {
            return Flux.just("错误：无法识别用户身份，请重新登录")
                    .concatWith(Flux.just("[complete]"));
        }

        // 生成或使用提供的会话ID
        final String finalSessionId = (sessionId != null && !sessionId.trim().isEmpty()) 
                ? sessionId 
                : "user-" + currentUserId + "-session-" + UUID.randomUUID().toString().substring(0, 8);

        // 创建一个final变量用于在lambda表达式中使用
        final Long finalCurrentUserId = currentUserId;

        // 在调用AI之前设置用户上下文到ThreadLocal
        UserContextHolder.setCurrentUserId(currentUserId);
        System.out.println("设置用户上下文到ThreadLocal - userId: " + currentUserId);
        
        // 设置UserContextHolder的用户上下文，LangChainBookingTools会自动获取
        UserContextHolder.setCurrentUserId(currentUserId);

        // 修改用户消息，添加用户ID信息，确保AI在调用函数时知道当前用户
        String enhancedMessage = message;
        if (!message.contains("用户ID") && !message.contains("userId")) {
            enhancedMessage = message + " (当前用户ID: " + finalCurrentUserId + ")";
        }

        // 如果启用RAG，从向量数据库中检索相关文档
        String ragContext = "";
        if (useRag) {
            List<Document> relevantDocuments = ragService.query(enhancedMessage, 0.7, 3);
            if (!relevantDocuments.isEmpty()) {
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("\n\n以下是来自知识库的相关信息，请参考这些信息来回答用户的问题：\n");
                for (int i = 0; i < relevantDocuments.size(); i++) {
                    Document doc = relevantDocuments.get(i);
                    contextBuilder.append("【知识库信息" + (i + 1) + "】: ").append(doc.getContent()).append("\n");
                }
                ragContext = contextBuilder.toString();
                System.out.println("RAG检索到 " + relevantDocuments.size() + " 个相关文档");
            }
        }
        
        // 组合最终的用户消息
        String finalMessage = enhancedMessage + ragContext;
        
        try {
            // 创建AI服务实例 - 使用LangChain4j的现代特性
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(bookingTools)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                    .systemMessageProvider((object) -> {
                        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
                        return """
                        您是"ie"智能日程管理助手的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                        您正在通过在线聊天系统与客户互动。
                        
                        重要提示：
                        1. 每个日程都有一个唯一的数字ID，这是在创建日程时系统自动生成的（存储在数据库表中）
                        2. 在执行取消或修改日程操作时，必须使用这个数字ID，而不是日程的状态或其他信息
                        3. 当用户想要修改或取消日程时，请先使用getBookings工具获取当前用户的所有日程
                        4. 然后根据用户描述匹配相应的日程，获取其ID
                        5. 最后使用获取到的ID执行相应的修改或取消操作
                        6. 用户不需要提供姓名，系统会自动使用当前登录用户的身份信息
                        7. 当用户询问日程时，直接告诉用户他的日程信息，不需要再询问姓名
                        8. 所有日程操作现在都基于用户ID进行，确保数据安全性和准确性
                        9. 当用户要求删除日程时，这将完全从数据库中移除记录，而不仅仅是更改状态
                        10. 取消操作会保留日程记录但将其状态设置为已取消，而删除操作会完全移除日程记录
                        11. 您可以使用以下工具来帮助用户：
                           - createBooking: 创建新的日程安排
                           - getBookings: 查询用户日程
                           - cancelBooking: 取消日程
                           - changeBooking: 修改日程
                           - checkConflict: 检测日程冲突
                        
                        请讲中文。
                        今天的日期是 %s。
                        """.formatted(currentDate);
                    })
                    .build();
            
            // 调用AI服务 - 使用MemoryId进行会话管理
            String response = assistant.chat(finalMessage, finalSessionId);
            
            // 保存对话记录到数据库
            try {
                conversationServiceImpl.saveConversation(
                    finalCurrentUserId, 
                    finalSessionId, 
                    message, 
                    response, 
                    "langchain4j_chat", 
                    "{}", 
                    true
                );
                System.out.println("对话记录保存成功 - 会话ID: " + finalSessionId);
            } catch (Exception e) {
                System.err.println("保存对话记录失败: " + e.getMessage());
            }
            
            // 将响应转换为Flux流
            return Flux.just(response)
                    .doOnNext(r -> System.out.println("LangChain4j AI响应: " + r))
                    .concatWith(Flux.just("[complete]"))
                    .doFinally(signal -> {
                        // 清理ThreadLocal，避免内存泄漏
                        UserContextHolder.clear();
                        System.out.println("LangChain4j接口调用完成 - 会话ID: " + finalSessionId);
                    });
                    
        } catch (Exception e) {
            System.err.println("LangChain4j AI调用错误: " + e.getMessage());
            e.printStackTrace();
            
            // 保存错误对话记录
            try {
                conversationServiceImpl.saveConversation(
                    finalCurrentUserId, 
                    finalSessionId, 
                    message, 
                    "抱歉，AI服务暂时不可用: " + e.getMessage(), 
                    "langchain4j_error", 
                    "{}", 
                    false
                );
            } catch (Exception ex) {
                System.err.println("保存错误对话记录失败: " + ex.getMessage());
            }
            
            return Flux.just("抱歉，AI服务暂时不可用: " + e.getMessage())
                    .concatWith(Flux.just("[complete]"))
                    .doFinally(signal -> {
                        // 清理ThreadLocal，避免内存泄漏
                        UserContextHolder.clear();
                        System.out.println("LangChain4j接口调用异常结束 - 会话ID: " + finalSessionId);
                    });
        }
    }

    /**
     * 获取当前登录用户的ID
     * @return 当前用户ID，如果无法获取则返回null
     */
    private Long getCurrentUserId() {
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
}