package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CalendarEvent {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String timezone;
    private String location;
    private EventType eventType;
    private Priority priority;
    private Boolean allDay;
    private String recurrenceRule;
    private String recurrenceExceptions;
    private Status status;
    private Visibility visibility;
    private String externalEventId;
    private String externalCalendarId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum EventType {
        MEETING, APPOINTMENT, TASK, REMINDER, PERSONAL, OTHER
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public enum Status {
        NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public enum Visibility {
        PUBLIC, PRIVATE, SHARED
    }
}