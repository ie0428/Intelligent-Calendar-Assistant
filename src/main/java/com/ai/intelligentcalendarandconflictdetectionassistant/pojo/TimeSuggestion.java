package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSuggestion {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private double confidence;
    private String reason;
}