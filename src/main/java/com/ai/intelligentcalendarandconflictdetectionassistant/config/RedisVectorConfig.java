package com.ai.intelligentcalendarandconflictdetectionassistant.config;

import com.ai.intelligentcalendarandconflictdetectionassistant.vectorstore.SimpleRedisVectorStore;
import lombok.AllArgsConstructor;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
// 禁用SpringAI提供的RedisStack向量数据库的自动配置，会和Redis的配置冲突。
@EnableAutoConfiguration(exclude = {RedisVectorStoreAutoConfiguration.class})
// 读取RedisStack的配置信息
@EnableConfigurationProperties({RedisVectorStoreProperties.class})
@AllArgsConstructor
public class RedisVectorConfig {

    /**
     * 创建基于普通Redis的向量数据库（不使用Redis Stack）
     *
     * @param embeddingModel 嵌入模型
     * @param properties     redis的配置信息
     * @param connectionFactory Redis连接工厂
     * @return vectorStore 向量数据库
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   RedisVectorStoreProperties properties,
                                   RedisConnectionFactory connectionFactory) {
        return new SimpleRedisVectorStore(embeddingModel, connectionFactory, properties.getPrefix());
    }
}