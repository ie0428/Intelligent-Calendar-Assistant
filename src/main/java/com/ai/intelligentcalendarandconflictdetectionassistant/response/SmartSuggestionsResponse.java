package com.ai.intelligentcalendarandconflictdetectionassistant.response;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.TimeSuggestion;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.UserPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartSuggestionsResponse {
    private LocalDate date;
    private List<TimeSuggestion> optimalSlots;
    private UserPreference userPreferences;
    private String message;
}