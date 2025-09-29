package com.ai.intelligentcalendarandconflictdetectionassistant.mapper;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.UserPreference;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

@Mapper
public interface UserPreferenceMapper {

    @Select("SELECT * FROM user_preferences WHERE user_id = #{userId}")
    Optional<UserPreference> findByUserId(Long userId);

    @Insert("INSERT INTO user_preferences(user_id, work_day_start, work_day_end, include_weekends, " +
            "default_event_duration, buffer_time_before_events, buffer_time_after_events, " +
            "default_reminder_time, theme, notification_enabled, email_notifications, created_at, updated_at) " +
            "VALUES(#{userId}, #{workDayStart}, #{workDayEnd}, #{includeWeekends}, " +
            "#{defaultEventDuration}, #{bufferTimeBeforeEvents}, #{bufferTimeAfterEvents}, " +
            "#{defaultReminderTime}, #{theme}, #{notificationEnabled}, #{emailNotifications}, " +
            "#{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserPreference preference);

    @Update("UPDATE user_preferences SET work_day_start=#{workDayStart}, work_day_end=#{workDayEnd}, " +
            "include_weekends=#{includeWeekends}, default_event_duration=#{defaultEventDuration}, " +
            "buffer_time_before_events=#{bufferTimeBeforeEvents}, buffer_time_after_events=#{bufferTimeAfterEvents}, " +
            "default_reminder_time=#{defaultReminderTime}, theme=#{theme}, " +
            "notification_enabled=#{notificationEnabled}, email_notifications=#{emailNotifications}, " +
            "updated_at=#{updatedAt} WHERE user_id=#{userId}")
    void update(UserPreference preference);

    @Delete("DELETE FROM user_preferences WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}