package com.ai.intelligentcalendarandconflictdetectionassistant.mapper;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.ConflictDetectionLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConflictDetectionLogMapper {

    @Select("SELECT * FROM conflict_detection_logs WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ConflictDetectionLog> findByUserId(Long userId, int limit);

    @Insert("INSERT INTO conflict_detection_logs(user_id, proposed_date, proposed_start_time, proposed_end_time, " +
            "has_conflict, conflict_count, severity, ai_suggestion_used, created_at) " +
            "VALUES(#{userId}, #{proposedDate}, #{proposedStartTime}, #{proposedEndTime}, " +
            "#{hasConflict}, #{conflictCount}, #{severity}, #{aiSuggestionUsed}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ConflictDetectionLog log);

    @Delete("DELETE FROM conflict_detection_logs WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);
}