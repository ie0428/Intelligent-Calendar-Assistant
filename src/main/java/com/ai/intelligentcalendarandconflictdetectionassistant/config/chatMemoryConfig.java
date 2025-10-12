package com.ai.intelligentcalendarandconflictdetectionassistant.config;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class chatMemoryConfig {
    
    @Bean
    public ChatMemory chatMemory()
    {
        return new InMemoryChatMemory();
    }
    
    /**
     * 配置LangChain4j的ChatMemoryStore
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }
}
