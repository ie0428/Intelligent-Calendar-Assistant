package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 确保用户存在，如果不存在则创建
     * @param userId 用户ID
     * @param username 用户名
     * @return 用户对象
     */
    User ensureUserExists(Long userId, String username);
    
    /**
     * 根据ID查找用户
     * @param userId 用户ID
     * @return 用户对象，如果不存在返回null
     */
    User findUserById(Long userId);
}