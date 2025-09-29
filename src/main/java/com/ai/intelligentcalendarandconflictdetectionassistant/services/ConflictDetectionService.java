package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request.ConflictCheckRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.response.ConflictCheckResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request.SmartSuggestionsRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.response.SmartSuggestionsResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.TimeSuggestion;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.CalendarEventMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.ConflictDetectionLogMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.UserPreferenceMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.ConflictDetectionLog;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.ConflictDetectionLog.Severity;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.UserPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConflictDetectionService {

    @Autowired
    private CalendarEventMapper calendarEventMapper;

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;

    @Autowired
    private ConflictDetectionLogMapper conflictDetectionLogMapper;

    /**
     * 检测日程冲突
     */
    public ConflictCheckResponse checkConflict(ConflictCheckRequest request, Long userId) {
        log.info("开始检测用户 {} 的日程冲突: {} {}", userId, request.getProposedDate(), request.getStartTime());
        
        // 获取用户当天的现有日程
        List<CalendarEvent> existingEvents = getEventsByDate(userId, request.getProposedDate());
        log.info("用户 {} 在 {} 有 {} 个现有日程", userId, request.getProposedDate(), existingEvents.size());
        
        // 转换时间格式
        LocalTime proposedStart = request.getStartTime();
        LocalTime proposedEnd = request.getEndTime();
        
        // 查找冲突的日程
        List<CalendarEvent> conflicts = findConflictingEvents(existingEvents, proposedStart, proposedEnd);
        log.info("检测到 {} 个冲突日程", conflicts.size());
        
        // 计算冲突严重程度
        Severity severity = calculateSeverity(conflicts, proposedStart, proposedEnd);
        
        // 生成建议
        List<TimeSuggestion> suggestions = conflicts.isEmpty() ? 
            Collections.emptyList() : generateSuggestions(request, existingEvents, userId);
        
        // 记录冲突检测日志
        recordConflictDetectionLog(userId, request, !conflicts.isEmpty(), conflicts.size(), severity);
        
        // 转换冲突事件为BookingDetails
        // List<BookingDetails> conflictDetails = conflicts.stream()
        //     .map(this::convertToBookingDetails)
        //     .collect(Collectors.toList());
        
        String message = conflicts.isEmpty() ? 
            "该时间段无冲突，可以安排日程" : 
            String.format("检测到 %d 个冲突日程，建议调整时间", conflicts.size());
        
        return ConflictCheckResponse.builder()
            .hasConflict(!conflicts.isEmpty())
            .conflictingEvents(conflicts)
            .severity(severity)
            .suggestions(suggestions)
            .message(message)
            .build();
    }

    /**
     * 获取智能建议
     */
    public SmartSuggestionsResponse getSmartSuggestions(SmartSuggestionsRequest request, Long userId) {
        log.info("为用户 {} 生成 {} 的智能建议，时长 {} 分钟", userId, request.getDate(), request.getDuration());
        
        // 获取用户偏好
        UserPreference preferences = getUserPreferences(userId);
        
        // 获取当天的日程
        List<CalendarEvent> dayEvents = getEventsByDate(userId, request.getDate());
        
        // 查找最佳时间段
        List<TimeSuggestion> optimalSlots = findOptimalTimeSlots(
            request.getDate(), request.getDuration(), dayEvents, preferences);
        
        return SmartSuggestionsResponse.builder()
            .date(request.getDate())
            .optimalSlots(optimalSlots)
            .userPreferences(preferences)
            .message(String.format("找到 %d 个最佳时间段", optimalSlots.size()))
            .build();
    }

    /**
     * 获取用户偏好（如果不存在则创建默认偏好）
     */
    private UserPreference getUserPreferences(Long userId) {
        return userPreferenceMapper.findByUserId(userId)
            .orElseGet(() -> createDefaultUserPreference(userId));
    }

    /**
     * 创建默认用户偏好
     */
    private UserPreference createDefaultUserPreference(Long userId) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setWorkDayStart(LocalTime.of(9, 0));
        preference.setWorkDayEnd(LocalTime.of(17, 0));
        preference.setIncludeWeekends(false);
        preference.setDefaultEventDuration(60);
        preference.setBufferTimeBeforeEvents(15);
        preference.setBufferTimeAfterEvents(15);
        preference.setDefaultReminderTime(30);
        preference.setTheme("light");
        preference.setNotificationEnabled(true);
        preference.setEmailNotifications(true);
        
        LocalDateTime now = LocalDateTime.now();
        preference.setCreatedAt(now);
        preference.setUpdatedAt(now);
        
        userPreferenceMapper.insert(preference);
        log.info("为用户 {} 创建默认偏好设置", userId);
        return preference;
    }

    /**
     * 获取指定日期的日程事件
     */
    private List<CalendarEvent> getEventsByDate(Long userId, LocalDate date) {
        return calendarEventMapper.findByUserId(userId).stream()
            .filter(event -> !event.getStatus().equals(CalendarEvent.Status.CANCELLED))
            .filter(event -> isEventOnDate(event, date))
            .collect(Collectors.toList());
    }

    /**
     * 检查事件是否在指定日期
     */
    private boolean isEventOnDate(CalendarEvent event, LocalDate date) {
        LocalDate eventDate = event.getStartTime().toLocalDate();
        return eventDate.equals(date);
    }

    /**
     * 查找冲突的事件
     */
    private List<CalendarEvent> findConflictingEvents(List<CalendarEvent> existingEvents, 
                                                       LocalTime proposedStart, LocalTime proposedEnd) {
        return existingEvents.stream()
            .filter(event -> hasTimeConflict(proposedStart, proposedEnd, event))
            .collect(Collectors.toList());
    }

    /**
     * 检查时间冲突
     */
    private boolean hasTimeConflict(LocalTime proposedStart, LocalTime proposedEnd, CalendarEvent event) {
        LocalTime eventStart = event.getStartTime().toLocalTime();
        LocalTime eventEnd = event.getEndTime().toLocalTime();
        
        // 考虑缓冲时间
        LocalTime bufferedStart = proposedStart.minusMinutes(15); // 默认15分钟缓冲
        LocalTime bufferedEnd = proposedEnd.plusMinutes(15);
        
        return !(bufferedEnd.isBefore(eventStart) || bufferedStart.isAfter(eventEnd));
    }

    /**
     * 计算冲突严重程度
     */
    private Severity calculateSeverity(List<CalendarEvent> conflicts, LocalTime proposedStart, LocalTime proposedEnd) {
        if (conflicts.isEmpty()) {
            return Severity.NONE;
        }
        
        long totalConflictMinutes = 0;
        for (CalendarEvent conflict : conflicts) {
            totalConflictMinutes += calculateConflictMinutes(proposedStart, proposedEnd, conflict);
        }
        
        if (totalConflictMinutes <= 15) {
            return Severity.MINOR;
        } else if (totalConflictMinutes <= 60) {
            return Severity.MODERATE;
        } else {
            return Severity.SEVERE;
        }
    }

    /**
     * 计算冲突分钟数
     */
    private long calculateConflictMinutes(LocalTime proposedStart, LocalTime proposedEnd, CalendarEvent event) {
        LocalTime eventStart = event.getStartTime().toLocalTime();
        LocalTime eventEnd = event.getEndTime().toLocalTime();
        
        LocalTime conflictStart = proposedStart.isAfter(eventStart) ? proposedStart : eventStart;
        LocalTime conflictEnd = proposedEnd.isBefore(eventEnd) ? proposedEnd : eventEnd;
        
        return Duration.between(conflictStart, conflictEnd).toMinutes();
    }

    /**
     * 生成时间建议
     */
    private List<TimeSuggestion> generateSuggestions(ConflictCheckRequest request, 
                                                     List<CalendarEvent> existingEvents, Long userId) {
        List<TimeSuggestion> suggestions = new ArrayList<>();
        
        // 获取用户偏好
        UserPreference preferences = getUserPreferences(userId);
        
        // 建议1: 同一天的相邻时间段
        suggestions.addAll(findAdjacentSlots(request.getProposedDate(), existingEvents, 
            Duration.between(request.getStartTime(), request.getEndTime()).toMinutes(), 
            request.getStartTime(), preferences));
        
        // 建议2: 第二天的相同时间段
        suggestions.addAll(findNextDaySlots(request.getProposedDate().plusDays(1), 
            Duration.between(request.getStartTime(), request.getEndTime()).toMinutes(), 
            request.getStartTime(), userId));
        
        // 建议3: 基于用户偏好的时间段
        suggestions.addAll(findOptimalTimeSlots(request.getProposedDate(), 
            Duration.between(request.getStartTime(), request.getEndTime()).toMinutes(), 
            existingEvents, preferences));
        
        // 排序并限制数量
        return suggestions.stream()
            .sorted((s1, s2) -> Double.compare(s2.getConfidence(), s1.getConfidence()))
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * 查找相邻时间段
     */
    private List<TimeSuggestion> findAdjacentSlots(LocalDate date, List<CalendarEvent> existingEvents, 
                                                    long duration, LocalTime originalTime, 
                                                    UserPreference preferences) {
        List<TimeSuggestion> slots = new ArrayList<>();
        
        // 按时间排序现有日程
        List<CalendarEvent> sortedEvents = existingEvents.stream()
            .sorted(Comparator.comparing(e -> e.getStartTime().toLocalTime()))
            .collect(Collectors.toList());
        
        // 工作日时间范围
        LocalTime workStart = preferences.getWorkDayStart();
        LocalTime workEnd = preferences.getWorkDayEnd();
        
        LocalTime current = workStart;
        
        for (CalendarEvent event : sortedEvents) {
            LocalTime eventStart = event.getStartTime().toLocalTime();
            LocalTime eventEnd = event.getEndTime().toLocalTime();
            
            // 检查当前时间段和事件之间的空闲时间
            if (Duration.between(current, eventStart).toMinutes() >= duration + preferences.getBufferTimeBeforeEvents()) {
                TimeSuggestion suggestion = TimeSuggestion.builder()
                    .date(date)
                    .startTime(current.plusMinutes(preferences.getBufferTimeBeforeEvents()))
                    .endTime(current.plusMinutes(preferences.getBufferTimeBeforeEvents() + duration))
                    .confidence(calculateConfidence(current, originalTime))
                    .reason("相邻空闲时段")
                    .build();
                slots.add(suggestion);
            }
            
            current = eventEnd.isAfter(current) ? eventEnd : current;
        }
        
        // 检查最后的时间段
        if (Duration.between(current, workEnd).toMinutes() >= duration + preferences.getBufferTimeAfterEvents()) {
            TimeSuggestion suggestion = TimeSuggestion.builder()
                .date(date)
                .startTime(current.plusMinutes(preferences.getBufferTimeAfterEvents()))
                .endTime(current.plusMinutes(preferences.getBufferTimeAfterEvents() + duration))
                .confidence(0.7)
                .reason("傍晚空闲时段")
                .build();
            slots.add(suggestion);
        }
        
        return slots;
    }

    /**
     * 查找第二天的时间段
     */
    private List<TimeSuggestion> findNextDaySlots(LocalDate nextDate, long duration, 
                                                 LocalTime originalTime, Long userId) {
        List<TimeSuggestion> slots = new ArrayList<>();
        
        // 获取用户偏好
        UserPreference preferences = getUserPreferences(userId);
        
        // 在相同时间前后1小时范围内查找
        for (int offset = -60; offset <= 60; offset += 30) {
            LocalTime suggestedStart = originalTime.plusMinutes(offset);
            LocalTime suggestedEnd = suggestedStart.plusMinutes(duration);
            
            if (suggestedStart.isAfter(preferences.getWorkDayStart()) && 
                suggestedEnd.isBefore(preferences.getWorkDayEnd())) {
                
                TimeSuggestion suggestion = TimeSuggestion.builder()
                    .date(nextDate)
                    .startTime(suggestedStart)
                    .endTime(suggestedEnd)
                    .confidence(calculateConfidence(suggestedStart, originalTime) * 0.8)
                    .reason("第二天相同时间段")
                    .build();
                slots.add(suggestion);
            }
        }
        
        return slots;
    }

    /**
     * 查找最佳时间段
     */
    private List<TimeSuggestion> findOptimalTimeSlots(LocalDate date, long duration, 
                                                     List<CalendarEvent> existingEvents, 
                                                     UserPreference preferences) {
        List<TimeSuggestion> slots = new ArrayList<>();
        
        // 基于用户偏好生成建议时间段
        LocalTime workStart = preferences.getWorkDayStart();
        LocalTime workEnd = preferences.getWorkDayEnd();
        
        // 检查上午时段（9:00-12:00）
        LocalTime morningStart = workStart.isBefore(LocalTime.of(9, 0)) ? LocalTime.of(9, 0) : workStart;
        LocalTime morningEnd = LocalTime.of(12, 0);
        
        if (Duration.between(morningStart, morningEnd).toMinutes() >= duration) {
            slots.add(TimeSuggestion.builder()
                .date(date)
                .startTime(morningStart)
                .endTime(morningStart.plusMinutes(duration))
                .confidence(0.8)
                .reason("上午最佳时段")
                .build());
        }
        
        // 检查下午时段（14:00-17:00）
        LocalTime afternoonStart = LocalTime.of(14, 0);
        LocalTime afternoonEnd = workEnd.isAfter(LocalTime.of(17, 0)) ? LocalTime.of(17, 0) : workEnd;
        
        if (Duration.between(afternoonStart, afternoonEnd).toMinutes() >= duration) {
            slots.add(TimeSuggestion.builder()
                .date(date)
                .startTime(afternoonStart)
                .endTime(afternoonStart.plusMinutes(duration))
                .confidence(0.75)
                .reason("下午最佳时段")
                .build());
        }
        
        return slots;
    }

    /**
     * 计算建议的置信度
     */
    private double calculateConfidence(LocalTime suggested, LocalTime original) {
        long timeDiff = Math.abs(Duration.between(suggested, original).toMinutes());
        if (timeDiff <= 30) return 0.9;
        if (timeDiff <= 60) return 0.7;
        if (timeDiff <= 120) return 0.5;
        return 0.3;
    }


    /**
     * 记录冲突检测日志
     */
    private void recordConflictDetectionLog(Long userId, ConflictCheckRequest request, 
                                          boolean hasConflict, int conflictCount, Severity severity) {
        try {
            ConflictDetectionLog log = new ConflictDetectionLog();
            log.setUserId(userId);
            log.setProposedDate(request.getProposedDate());
            log.setProposedStartTime(request.getStartTime());
            log.setProposedEndTime(request.getEndTime());
            log.setHasConflict(hasConflict);
            log.setConflictCount(conflictCount);
            log.setSeverity(severity);
            log.setAiSuggestionUsed(false); // 可以后续扩展AI建议功能
            log.setCreatedAt(LocalDateTime.now());
            
            conflictDetectionLogMapper.insert(log);
            this.log.info("记录冲突检测日志成功: userId={}, hasConflict={}, severity={}", 
                    userId, hasConflict, severity);
        } catch (Exception e) {
            this.log.error("记录冲突检测日志失败", e);
        }
    }
}