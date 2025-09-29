package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request.ConflictCheckRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.response.ConflictCheckResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request.SmartSuggestionsRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.response.SmartSuggestionsResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConflictDetectionService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // 修复导入错误，使用jakarta.validation替代javax.validation
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/conflict")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ConflictDetectionController {

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    /**
     * 检测日程冲突
     */
    @PostMapping("/check")
    public ResponseEntity<ConflictCheckResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        try {
            Long userId = getCurrentUserId();
            log.info("用户 {} 请求检测冲突: {} {}-{}", 
                    userId, request.getProposedDate(), request.getStartTime(), request.getEndTime());
            
            ConflictCheckResponse response = conflictDetectionService.checkConflict(request, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检测冲突失败", e);
            return ResponseEntity.badRequest().body(
                ConflictCheckResponse.builder()
                    .hasConflict(false)
                    .message("检测冲突失败: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 获取智能时间建议
     */
    @GetMapping("/suggestions")
    public ResponseEntity<SmartSuggestionsResponse> getSmartSuggestions(
            @RequestParam String date,
            @RequestParam(defaultValue = "60") int duration,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String location) {
        
        try {
            Long userId = getCurrentUserId();
            LocalDate localDate = LocalDate.parse(date);
            
            log.info("用户 {} 请求智能建议: {} 时长 {} 分钟", userId, localDate, duration);
            
            SmartSuggestionsRequest request = new SmartSuggestionsRequest();
            request.setDate(localDate);
            request.setDuration(duration);
            request.setEventType(eventType);
            request.setLocation(location);
            
            SmartSuggestionsResponse response = conflictDetectionService.getSmartSuggestions(request, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取智能建议失败", e);
            return ResponseEntity.badRequest().body(
                SmartSuggestionsResponse.builder()
                    .message("获取智能建议失败: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        // 如果没有认证信息，返回默认用户ID（用于测试）
        log.warn("无法获取当前用户ID，使用默认用户ID: 3");
        return 3L;
    }
}