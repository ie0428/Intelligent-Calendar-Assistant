package com.ai.intelligentcalendarandconflictdetectionassistant.response;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.TimeSuggestion;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.ConflictDetectionLog.Severity;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictCheckResponse {
    private boolean hasConflict;
    private List<CalendarEvent> conflictingEvents;
    private Severity severity;
    private List<TimeSuggestion> suggestions;
    private String message;
}