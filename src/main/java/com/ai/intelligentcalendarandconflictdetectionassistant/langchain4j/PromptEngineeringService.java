package com.ai.intelligentcalendarandconflictdetectionassistant.langchain4j;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 提示工程优化服务
 */
@Service
@AllArgsConstructor
public class PromptEngineeringService {

    private final EnhancedChatMemoryService chatMemoryService;
    private final EnhancedRAGService ragService;

    /**
     * 生成智能系统提示词
     */
    public String generateSmartSystemPrompt(String sessionId, String userMessage) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        
        // 获取对话记忆摘要
        String memorySummary = chatMemoryService.getMemorySummary(sessionId);
        
        // 分析用户意图
        String intent = analyzeUserIntent(userMessage);
        
        // 生成角色描述
        String roleDescription = generateRoleDescription(intent);
        
        // 构建动态提示词
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(roleDescription).append("\n\n");
        promptBuilder.append("当前日期：").append(currentDate).append("\n");
        
        if (!memorySummary.equals("无对话历史")) {
            promptBuilder.append("对话历史：\n").append(memorySummary).append("\n");
        }
        
        promptBuilder.append("\n重要提示：\n");
        promptBuilder.append(getImportantTips(intent));
        
        promptBuilder.append("\n可用工具：\n");
        promptBuilder.append(getAvailableTools(intent));
        
        promptBuilder.append("\n请讲中文，并以友好、乐于助人的方式回复。");
        
        return promptBuilder.toString();
    }

    /**
     * 分析用户意图
     */
    private String analyzeUserIntent(String userMessage) {
        userMessage = userMessage.toLowerCase();
        
        if (userMessage.contains("创建") || userMessage.contains("安排") || userMessage.contains("添加")) {
            return "CREATE";
        } else if (userMessage.contains("查询") || userMessage.contains("查看") || userMessage.contains("显示")) {
            return "QUERY";
        } else if (userMessage.contains("修改") || userMessage.contains("调整") || userMessage.contains("更改")) {
            return "MODIFY";
        } else if (userMessage.contains("取消") || userMessage.contains("删除")) {
            return "CANCEL";
        } else if (userMessage.contains("冲突") || userMessage.contains("时间冲突")) {
            return "CONFLICT";
        } else {
            return "GENERAL";
        }
    }

    /**
     * 生成角色描述
     */
    private String generateRoleDescription(String intent) {
        Map<String, String> roleDescriptions = new HashMap<>();
        roleDescriptions.put("CREATE", "您是专业的日程安排专家，擅长帮助用户创建和安排日程。");
        roleDescriptions.put("QUERY", "您是专业的日程查询专家，能够快速准确地查找和展示用户日程。");
        roleDescriptions.put("MODIFY", "您是专业的日程调整专家，擅长修改和优化现有日程安排。");
        roleDescriptions.put("CANCEL", "您是专业的日程管理专家，能够妥善处理日程取消和删除操作。");
        roleDescriptions.put("CONFLICT", "您是专业的冲突检测专家，能够识别和解决时间冲突问题。");
        roleDescriptions.put("GENERAL", "您是'ie'智能日程管理助手的客户聊天支持代理。");
        
        return roleDescriptions.getOrDefault(intent, roleDescriptions.get("GENERAL"));
    }

    /**
     * 获取重要提示
     */
    private String getImportantTips(String intent) {
        StringBuilder tips = new StringBuilder();
        
        // 通用提示
        tips.append("1. 每个日程都有唯一的数字ID，在执行操作时必须使用ID\n");
        tips.append("2. 系统会自动使用当前登录用户的身份信息\n");
        tips.append("3. 所有日程操作都基于用户ID进行，确保数据安全\n");
        
        // 意图特定提示
        switch (intent) {
            case "CREATE":
                tips.append("4. 创建日程时请确保时间信息完整准确\n");
                tips.append("5. 系统会自动检测时间冲突并提供建议\n");
                break;
            case "MODIFY":
                tips.append("4. 修改日程前请先确认要修改的日程ID\n");
                tips.append("5. 修改操作会保留原始记录，创建新的版本\n");
                break;
            case "CANCEL":
                tips.append("4. 取消操作会保留记录但更改状态\n");
                tips.append("5. 删除操作会完全移除日程记录\n");
                break;
            case "CONFLICT":
                tips.append("4. 冲突检测基于时间重叠算法\n");
                tips.append("5. 系统会提供智能时间建议\n");
                break;
        }
        
        return tips.toString();
    }

    /**
     * 获取可用工具描述
     */
    private String getAvailableTools(String intent) {
        StringBuilder tools = new StringBuilder();
        
        tools.append("- createBooking: 创建新的日程安排\n");
        tools.append("- getBookings: 查询用户日程\n");
        tools.append("- cancelBooking: 取消日程\n");
        tools.append("- changeBooking: 修改日程\n");
        tools.append("- checkConflict: 检测日程冲突\n");
        
        // 推荐工具
        switch (intent) {
            case "CREATE":
                tools.append("\n推荐工具：createBooking, checkConflict\n");
                break;
            case "QUERY":
                tools.append("\n推荐工具：getBookings\n");
                break;
            case "MODIFY":
                tools.append("\n推荐工具：getBookings, changeBooking\n");
                break;
            case "CANCEL":
                tools.append("\n推荐工具：getBookings, cancelBooking\n");
                break;
            case "CONFLICT":
                tools.append("\n推荐工具：checkConflict\n");
                break;
        }
        
        return tools.toString();
    }

    /**
     * 生成RAG增强的用户消息
     */
    public String generateRAGEnhancedMessage(String sessionId, String userMessage) {
        // 获取对话上下文
        String context = chatMemoryService.getMemorySummary(sessionId);
        
        // 使用RAG服务增强消息
        return ragService.getRAGEnhancedPrompt(userMessage, context);
    }

    /**
     * 生成工具调用前的预处理消息
     */
    public String preprocessForToolCalling(String sessionId, String userMessage, String toolName) {
        // 结合对话记忆和工具上下文
        String memoryContext = chatMemoryService.getMemorySummary(sessionId);
        String enhancedMessage = ragService.preprocessForToolCalling(userMessage, toolName);
        
        return enhancedMessage + "\n\n对话上下文：" + memoryContext;
    }
}