package com.ai.intelligentcalendarandconflictdetectionassistant.mapper;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConversationMapper {

    @Select("SELECT id, user_id, session_id, user_message, ai_response, intent, entities, successful, created_at " +
            "FROM conversations WHERE id = #{id}")
    Conversation findById(Long id);

    @Select("SELECT id, user_id, session_id, user_message, ai_response, intent, entities, successful, created_at " +
            "FROM conversations WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT id, user_id, session_id, user_message, ai_response, intent, entities, successful, created_at " +
            "FROM conversations WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Insert("INSERT INTO conversations(user_id, session_id, user_message, ai_response, intent, entities, successful, created_at) " +
            "VALUES(#{userId}, #{sessionId}, #{userMessage}, #{aiResponse}, #{intent}, #{entities}, #{successful}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Conversation conversation);

    @Delete("DELETE FROM conversations WHERE id = #{id}")
    void deleteById(Long id);

    @Select("SELECT * FROM conversations WHERE user_id = #{userId}")
    List<Conversation> findByUserId(Long userId);

    @Select("SELECT * FROM conversations WHERE session_id = #{sessionId}")
    List<Conversation> findBySessionId(String sessionId);
    
    @Select("SELECT DISTINCT session_id FROM conversations WHERE user_id = #{userId}")
    List<String> findDistinctSessionIdsByUserId(Long userId);
    
    @Select("SELECT COUNT(*) FROM conversations WHERE session_id = #{sessionId}")
    int countBySessionId(String sessionId);
    
    @Select("SELECT session_id FROM conversations WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    String findLatestSessionIdByUserId(Long userId);
    
    @Select("SELECT COUNT(*) > 0 FROM conversations WHERE session_id = #{sessionId}")
    boolean existsBySessionId(String sessionId);
}
