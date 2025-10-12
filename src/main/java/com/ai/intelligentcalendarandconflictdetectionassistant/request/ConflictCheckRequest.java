package com.ai.intelligentcalendarandconflictdetectionassistant.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ConflictCheckRequest {
    private String eventTitle;
    private LocalDate proposedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String description;
}