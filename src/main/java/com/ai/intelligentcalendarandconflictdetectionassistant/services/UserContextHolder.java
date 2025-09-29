package com.ai.intelligentcalendarandconflictdetectionassistant.services;

/**
 * 用户上下文持有者，用于在AI函数调用链中传递用户ID
 * 由于Spring AI的函数调用在独立线程中执行，无法直接访问SecurityContext
 * 使用InheritableThreadLocal确保子线程可以继承父线程的上下文
 */
public class UserContextHolder {
    
    private static final InheritableThreadLocal<Long> userIdThreadLocal = new InheritableThreadLocal<>();
    
    /**
     * 设置当前线程的用户ID
     * @param userId 用户ID
     */
    public static void setCurrentUserId(Long userId) {
        userIdThreadLocal.set(userId);
        // 只在首次设置或用户ID变化时输出日志，减少重复日志
        if (userId != null) {
            System.out.println("UserContextHolder设置用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
        }
    }
    
    /**
     * 获取当前线程的用户ID
     * @return 用户ID，如果没有设置则返回null
     */
    public static Long getCurrentUserId() {
        return userIdThreadLocal.get();
    }
    
    /**
     * 清除当前线程的用户ID
     */
    public static void clear() {
        Long userId = userIdThreadLocal.get();
        userIdThreadLocal.remove();
        // 只在有用户ID时输出清理日志
        if (userId != null) {
            System.out.println("UserContextHolder清理用户ID: " + userId + " - 线程: " + Thread.currentThread().getName());
        }
    }
}