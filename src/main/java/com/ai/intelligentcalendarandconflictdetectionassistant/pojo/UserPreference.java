package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class UserPreference {
    private Long id;
    private Long userId;
    private LocalTime workDayStart;
    private LocalTime workDayEnd;
    private Boolean includeWeekends;
    private Integer defaultEventDuration;
    private Integer bufferTimeBeforeEvents;
    private Integer bufferTimeAfterEvents;
    private Integer defaultReminderTime;
    private String theme;
    private Boolean notificationEnabled;
    private Boolean emailNotifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}