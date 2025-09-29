package com.ai.intelligentcalendarandconflictdetectionassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 线程池配置，确保AI函数调用能够继承用户上下文
 */
@Configuration
public class ThreadPoolConfig {
    
    @Bean(name = "aiFunctionExecutor")
    public TaskExecutor aiFunctionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-function-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 关键：设置为可继承的线程本地变量
        executor.setThreadFactory(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("ai-function-" + thread.getId());
            return thread;
        });
        executor.initialize();
        return executor;
    }
}