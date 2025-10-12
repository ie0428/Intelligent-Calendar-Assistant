package com.ai.intelligentcalendarandconflictdetectionassistant.langchain4j;

import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.ConflictDetectionServiceImpl;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.FlightBookingServiceImpl;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import com.ai.intelligentcalendarandconflictdetectionassistant.request.ConflictCheckRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.response.ConflictCheckResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.TimeSuggestion;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.BookingTools.BookingDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * LangChain4j 日程管理工具类
 * 使用@Tool注解封装基础的日程管理功能
 */
@Slf4j
@Component
public class LangChainBookingTools {

    @Autowired
    private FlightBookingServiceImpl flightBookingServiceImpl;

    @Autowired
    private ConflictDetectionServiceImpl conflictDetectionServiceImpl;

    /**
     * 创建新的日程安排
     */
    @Tool("创建新的日程安排")
    public String createBooking(
            @P("日程标题") String title,
            @P("日期，格式yyyy-MM-dd") String date,
            @P("开始时间，格式HH:mm") String startTime,
            @P("结束时间，格式HH:mm") String endTime,
            @P("地点，可选") String location,
            @P("描述，可选") String description
    ) {
        try {
            log.info("开始创建日程，标题: {}, 日期: {}, 时间: {} - {}", title, date, startTime, endTime);

            // 获取当前用户ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                userId = 3L; // 默认用户ID
                log.warn("无法获取用户ID，使用默认用户ID: {}", userId);
            }

            // 处理可选参数
            if (location == null || location.trim().isEmpty()) {
                location = "未指定地点";
            }
            if (description == null || description.trim().isEmpty()) {
                description = "未提供描述";
            }

            // 先进行冲突检测
            try {
                log.info("开始冲突检测，用户ID: {}, 日期: {}, 时间: {} - {}", userId, date, startTime, endTime);
                ConflictCheckRequest conflictRequest = new ConflictCheckRequest();
                conflictRequest.setEventTitle(title);
                conflictRequest.setProposedDate(LocalDate.parse(date));
                conflictRequest.setStartTime(LocalTime.parse(startTime));
                conflictRequest.setEndTime(LocalTime.parse(endTime));
                conflictRequest.setLocation(location);
                conflictRequest.setDescription(description);
                ConflictCheckResponse conflictResponse = conflictDetectionServiceImpl.checkConflict(conflictRequest, userId);

                if (conflictResponse.isHasConflict()) {
                    log.warn("检测到冲突，冲突数量: {}, 严重程度: {}",
                            conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());
                    String conflictMessage = String.format("检测到%d个冲突事件，严重程度: %s。",
                            conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());

                    if (conflictResponse.getSuggestions() != null && !conflictResponse.getSuggestions().isEmpty()) {
                        conflictMessage += " 建议时间: ";
                        for (TimeSuggestion suggestion : conflictResponse.getSuggestions()) {
                            conflictMessage += String.format("%s %s-%s (置信度: %.0f%%) ",
                                    suggestion.getDate(), suggestion.getStartTime(), suggestion.getEndTime(),
                                    suggestion.getConfidence() * 100);
                        }
                    }

                    log.info("返回冲突检测结果: {}", conflictMessage);
                    return "日程创建失败: " + conflictMessage;
                }

                log.info("未检测到冲突，继续创建日程");
            } catch (Exception e) {
                log.error("冲突检测失败，继续创建日程: {}", e.getMessage(), e);
            }

            flightBookingServiceImpl.createBooking(date, location, description, title, userId, "UTC");
            log.info("日程创建成功");
            return "日程创建成功";
        } catch (Exception e) {
            log.error("创建日程失败: ", e);
            return "日程创建失败: " + e.getMessage();
        }
    }

    /**
     * 查询用户日程
     */
    @Tool("查询用户日程")
    public List<BookingDetails> getBookings(
            @P("查询日期，格式yyyy-MM-dd，可选") String date
    ) {
        try {
            log.info("开始查询用户日程，日期: {}", date);

            // 获取当前用户ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                userId = 3L; // 默认用户ID
                log.warn("无法获取用户ID，使用默认用户ID: {}", userId);
            }

            // 目前只支持查询所有日程，后续可以添加按日期查询功能
            log.info("查询所有日程，用户ID: {}", userId);
            return flightBookingServiceImpl.getBookingsByUserId(userId);
        } catch (Exception e) {
            log.error("查询用户日程失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 取消日程
     */
    @Tool("取消日程")
    public String cancelBooking(
            @P("日程ID") String eventId
    ) {
        try {
            log.info("开始取消日程，事件ID: {}", eventId);

            // 获取当前用户ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                userId = 3L; // 默认用户ID
                log.warn("无法获取用户ID，使用默认用户ID: {}", userId);
            }

            flightBookingServiceImpl.cancelBookingByUserId(eventId, userId);
            log.info("日程取消成功");
            return "日程取消成功";
        } catch (Exception e) {
            log.error("取消日程失败: ", e);
            return "日程取消失败: " + e.getMessage();
        }
    }

    /**
     * 修改日程
     */
    @Tool("修改日程")
    public String changeBooking(
            @P("日程ID") String eventId,
            @P("新日期，格式yyyy-MM-dd") String date,
            @P("新地点") String location,
            @P("新描述") String description
    ) {
        try {
            log.info("开始修改日程，事件ID: {}, 日期: {}, 地点: {}, 描述: {}", eventId, date, location, description);

            // 获取当前用户ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                userId = 3L; // 默认用户ID
                log.warn("无法获取用户ID，使用默认用户ID: {}", userId);
            }

            // 先进行冲突检测
            try {
                log.info("开始冲突检测，用户ID: {}, 日期: {}", userId, date);
                ConflictCheckRequest conflictRequest = new ConflictCheckRequest();
                conflictRequest.setEventTitle("修改的日程");
                conflictRequest.setProposedDate(LocalDate.parse(date));
                conflictRequest.setStartTime(LocalTime.of(9, 0)); // 默认开始时间
                conflictRequest.setEndTime(LocalTime.of(17, 0)); // 默认结束时间
                conflictRequest.setLocation(location);
                conflictRequest.setDescription(description);
                ConflictCheckResponse conflictResponse = conflictDetectionServiceImpl.checkConflict(conflictRequest, userId);

                if (conflictResponse.isHasConflict()) {
                    log.warn("检测到冲突，冲突数量: {}, 严重程度: {}",
                            conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());
                    String conflictMessage = String.format("检测到%d个冲突事件，严重程度: %s。",
                            conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());

                    if (conflictResponse.getSuggestions() != null && !conflictResponse.getSuggestions().isEmpty()) {
                        conflictMessage += " 建议时间: ";
                        for (TimeSuggestion suggestion : conflictResponse.getSuggestions()) {
                            conflictMessage += String.format("%s %s-%s (置信度: %.0f%%) ",
                                    suggestion.getDate(), suggestion.getStartTime(), suggestion.getEndTime(),
                                    suggestion.getConfidence() * 100);
                        }
                    }

                    log.info("返回冲突检测结果: {}", conflictMessage);
                    return "日程修改失败: " + conflictMessage;
                }

                log.info("未检测到冲突，继续修改日程");
            } catch (Exception e) {
                log.error("冲突检测失败，继续修改日程: {}", e.getMessage(), e);
            }

            flightBookingServiceImpl.changeBookingByUserId(eventId, userId, date, location, description);
            log.info("日程修改成功");
            return "日程修改成功";
        } catch (Exception e) {
            log.error("修改日程失败: ", e);
            return "日程修改失败: " + e.getMessage();
        }
    }

    /**
     * 检测日程冲突
     */
    @Tool("检测日程冲突")
    public String checkConflict(
            @P("日程标题") String title,
            @P("日期，格式yyyy-MM-dd") String date,
            @P("开始时间，格式HH:mm") String startTime,
            @P("结束时间，格式HH:mm") String endTime,
            @P("地点，可选") String location
    ) {
        try {
            log.info("开始冲突检测，标题: {}, 日期: {}, 时间: {} - {}", title, date, startTime, endTime);

            // 获取当前用户ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                userId = 3L; // 默认用户ID
                log.warn("无法获取用户ID，使用默认用户ID: {}", userId);
            }

            // 处理可选参数
            if (location == null || location.trim().isEmpty()) {
                location = "未指定地点";
            }

            ConflictCheckRequest conflictRequest = new ConflictCheckRequest();
            conflictRequest.setEventTitle(title);
            conflictRequest.setProposedDate(LocalDate.parse(date));
            conflictRequest.setStartTime(LocalTime.parse(startTime));
            conflictRequest.setEndTime(LocalTime.parse(endTime));
            conflictRequest.setLocation(location);
            conflictRequest.setDescription("冲突检测");

            ConflictCheckResponse conflictResponse = conflictDetectionServiceImpl.checkConflict(conflictRequest, userId);

            if (conflictResponse.isHasConflict()) {
                String conflictMessage = String.format("检测到%d个冲突事件，严重程度: %s。",
                        conflictResponse.getConflictingEvents() != null ? conflictResponse.getConflictingEvents().size() : 0, conflictResponse.getSeverity());

                if (conflictResponse.getSuggestions() != null && !conflictResponse.getSuggestions().isEmpty()) {
                    conflictMessage += "\\n建议时间: ";
                    for (TimeSuggestion suggestion : conflictResponse.getSuggestions()) {
                        conflictMessage += String.format("%s %s-%s (置信度: %.0f%%)\\n",
                                suggestion.getDate(), suggestion.getStartTime(), suggestion.getEndTime(),
                                suggestion.getConfidence() * 100);
                    }
                }

                log.info("冲突检测结果: {}", conflictMessage);
                return conflictMessage;
            } else {
                log.info("未检测到冲突");
                return "未检测到冲突，可以安排此日程。";
            }
        } catch (Exception e) {
            log.error("冲突检测失败: ", e);
            return "冲突检测失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            // 首先尝试从UserContextHolder获取用户ID
            Long userId = com.ai.intelligentcalendarandconflictdetectionassistant.services.UserContextHolder.getCurrentUserId();
            if (userId != null) {
                log.info("从UserContextHolder获取到用户ID: {}", userId);
                return userId;
            }
            
            // 如果UserContextHolder中没有，尝试从SecurityContext获取
            org.springframework.security.core.context.SecurityContext securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
            if (securityContext != null && securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated()) {
                Object principal = securityContext.getAuthentication().getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    // 这里可以根据用户名获取用户ID，暂时返回默认值
                    log.info("从SecurityContext获取到用户名: {}", username);
                    return 3L; // 默认用户ID
                }
            }
            
            // 如果都无法获取，返回默认值
            log.warn("无法获取用户ID，使用默认用户ID: 3");
            return 3L;
        } catch (Exception e) {
            log.error("获取当前用户ID失败: {}", e.getMessage());
            return 3L; // 失败时返回默认值
        }
    }


}