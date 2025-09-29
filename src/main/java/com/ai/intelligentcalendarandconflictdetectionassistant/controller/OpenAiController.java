package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.DatabaseChatMemoryAdvisor;
import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.loggingAdvisor;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.UserContextHolder;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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


/**
 * @author xushu
 * @version 1.0
 * @description:
 */
@RestController
@CrossOrigin
public class OpenAiController {
    private final ChatClient chatClient;

    // 配置ChatClient
    public OpenAiController(ChatClient.Builder chatClientBuilder,
                            ChatMemory chatMemory,
                            ConversationService conversationService) {
        // 获取当前日期并格式化
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        您是"ie"智能日程管理助手的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                        您正在通过在线聊天系统与客户互动。
                        
                        重要提示：
                        1. 每个日程都有一个唯一的数字ID，这是在创建日程时系统自动生成的（存储在数据库表中）
                        2. 在执行取消或修改日程操作时，必须使用这个数字ID，而不是日程的状态或其他信息
                        3. 当用户想要修改或取消日程时，请先使用findCalendarEvent函数获取当前用户的所有日程
                        4. 然后根据用户描述匹配相应的日程，获取其ID
                        5. 最后使用获取到的ID执行相应的修改或取消操作
                        6. 用户不需要提供姓名，系统会自动使用当前登录用户的身份信息
                        7. 当用户询问日程时，直接告诉用户他的日程信息，不需要再询问姓名
                        8. 所有日程操作现在都基于用户ID进行，确保数据安全性和准确性
                        9. 当用户要求删除日程时，这将完全从数据库中移除记录，而不仅仅是更改状态
                        10. 取消操作会保留日程记录但将其状态设置为已取消，而删除操作会完全移除日程记录
                        
                        请讲中文。
                        今天的日期是 %s.
                    """.formatted(currentDate))
                .defaultAdvisors(new loggingAdvisor())
                .defaultAdvisors(new DatabaseChatMemoryAdvisor(conversationService))
                .defaultFunctions("cancelBooking","getBookingDetails","createBooking","changeBooking","findCalendarEvent","getAllBookings","getSmartScheduleSuggestions","deleteBooking")
                .build();
    }
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "讲个笑话") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "userId", required = false) Long userId) {

        // 优先使用URL参数中的userId，如果没有则尝试从认证中获取
        final Long currentUserId;
        if (userId != null) {
            currentUserId = userId;
        } else {
            currentUserId = getCurrentUserId();
        }
        
        System.out.println("当前登录用户ID: " + currentUserId + ", sessionId: " + sessionId);

        // 如果无法获取用户ID，返回错误信息
        if (currentUserId == null) {
            return Flux.just("错误：无法识别用户身份，请重新登录")
                    .concatWith(Flux.just("[complete]"));
        }

        // 创建一个final变量用于在lambda表达式中使用
        final Long finalCurrentUserId = currentUserId;

        // 在调用AI之前设置用户上下文到ThreadLocal
        UserContextHolder.setCurrentUserId(currentUserId);
        // 只在首次设置时输出日志，减少重复输出
        System.out.println("设置用户上下文到ThreadLocal - userId: " + currentUserId);
        
        // 同时设置BookingTools的当前请求用户ID
        BookingTools.setCurrentRequestUserId(currentUserId);
        
        // 设置AI函数调用的默认参数
        Map<String, Object> functionContext = new HashMap<>();
        functionContext.put("userId", currentUserId);
        functionContext.put("requestThread", Thread.currentThread().getName());

        // 修改用户消息，添加用户ID信息，确保AI在调用函数时知道当前用户
        String enhancedMessage = message;
        if (!message.contains("用户ID") && !message.contains("userId")) {
            enhancedMessage = message + " (当前用户ID: " + finalCurrentUserId + ")";
        }
        
        // 获取当前日期并格式化
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // 系统提示词，指导AI在调用函数时使用正确的用户ID
        String systemPrompt = "你是一个智能助手。当用户询问日程信息时，你可以调用相关函数。" +
                "重要：在调用getAllBookings函数时，必须使用当前用户的ID（" + finalCurrentUserId + "）。" +
                "当前日期是: " + currentDate;
        
        Flux<String> content = chatClient.prompt()
                .user(enhancedMessage)
                .system(systemPrompt)
                .advisors(request -> {
                    request.param("sessionId", sessionId);
                    request.param("userId", finalCurrentUserId);
                })// 传递sessionId和userId给advisor
                .stream()
                .content()
                .doFinally(signal -> {
                    // 清理ThreadLocal，避免内存泄漏
                    UserContextHolder.clear();
                    // 同时清理BookingTools的当前请求用户ID
                    BookingTools.clearCurrentRequestUserId();
                })
                .doOnError(error -> {
                    System.err.println("AI调用错误: " + error.getMessage());
                    error.printStackTrace();
                });

        return  content
                .doOnNext(response -> System.out.print(response))
                .doOnComplete(() -> System.out.println("\n[AI响应完成]"))
                .concatWith(Flux.just("[complete]"));
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