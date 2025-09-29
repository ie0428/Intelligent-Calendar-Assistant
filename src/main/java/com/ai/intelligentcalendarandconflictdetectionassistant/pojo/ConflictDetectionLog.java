package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class ConflictDetectionLog {
    private Long id;
    private Long userId;
    private LocalDate proposedDate;
    private LocalTime proposedStartTime;
    private LocalTime proposedEndTime;
    private Boolean hasConflict;
    private Integer conflictCount;
    private Severity severity;
    private Boolean aiSuggestionUsed;
    private LocalDateTime createdAt;

    public enum Severity {
        NONE, MINOR, MODERATE, SEVERE
    }
}