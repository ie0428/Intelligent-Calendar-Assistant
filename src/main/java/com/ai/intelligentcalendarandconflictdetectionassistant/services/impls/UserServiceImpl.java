// UserService.java
package com.ai.intelligentcalendarandconflictdetectionassistant.services.impls;

import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.UserMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl {

    @Autowired
    private UserMapper userMapper;

    /**
     * 确保用户存在，如果不存在则创建
     * @param userId 用户ID
     * @param username 用户名
     * @return 用户对象
     */
    public User ensureUserExists(Long userId, String username) {
        User user = null;

        // 首先尝试通过ID查找用户
        if (userId != null) {
            user = userMapper.findById(userId);
        }

        // 如果通过ID没找到，尝试通过用户名查找
        if (user == null && username != null) {
            user = userMapper.findByUsername(username);
        }

        // 如果都找不到，则创建新用户
        if (user == null) {
            user = new User();
            user.setId(userId);
            user.setUsername(username);
            userMapper.insert(user);
        }

        return user;
    }

    /**
     * 根据ID查找用户
     * @param userId 用户ID
     * @return 用户对象，如果不存在返回null
     */
    public User findUserById(Long userId) {
        return userMapper.findById(userId);
    }
}

