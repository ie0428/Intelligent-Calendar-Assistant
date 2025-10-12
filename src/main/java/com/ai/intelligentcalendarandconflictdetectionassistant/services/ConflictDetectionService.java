package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.request.ConflictCheckRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.response.ConflictCheckResponse;
import com.ai.intelligentcalendarandconflictdetectionassistant.request.SmartSuggestionsRequest;
import com.ai.intelligentcalendarandconflictdetectionassistant.response.SmartSuggestionsResponse;

/**
 * 冲突检测服务接口
 */
public interface ConflictDetectionService {
    
    /**
     * 检测日程冲突
     * @param request 冲突检测请求
     * @param userId 用户ID
     * @return 冲突检测响应
     */
    ConflictCheckResponse checkConflict(ConflictCheckRequest request, Long userId);
    
    /**
     * 获取智能建议
     * @param request 智能建议请求
     * @param userId 用户ID
     * @return 智能建议响应
     */
    SmartSuggestionsResponse getSmartSuggestions(SmartSuggestionsRequest request, Long userId);
}