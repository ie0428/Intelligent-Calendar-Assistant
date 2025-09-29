package com.ai.intelligentcalendarandconflictdetectionassistant.pojo.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SmartSuggestionsRequest {
    private LocalDate date;
    private int duration; // 分钟
    private String eventType;
    private String location;
}