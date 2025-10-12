package com.ai.intelligentcalendarandconflictdetectionassistant.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SmartSuggestionsRequest {
    private LocalDate date;
    private int duration; // 分钟
    private String eventType;
    private String location;
}