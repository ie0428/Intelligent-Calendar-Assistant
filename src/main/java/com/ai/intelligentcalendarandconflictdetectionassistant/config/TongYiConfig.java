package com.ai.intelligentcalendarandconflictdetectionassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * 通义千问配置类
 * 封装通义千问为LangChain4j的ChatLanguageModel和EmbeddingModel
 */
@Configuration
public class TongYiConfig {

    @Value("${TONGYI_AI_KEY}")
    private String apiKey;

    @Value("${spring.ai.dashscope.chat.options.model:qwen3-max}")
    private String modelName;

    @Value("${tongyi.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${tongyi.temperature:0.7}")
    private Double temperature;

    @Value("${tongyi.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${tongyi.timeout:60}")
    private Integer timeout;

    @Value("${tongyi.embedding-model:text-embedding-v1}")
    private String embeddingModelName;

    /**
     * 创建通义千问的ChatLanguageModel
     * 使用OpenAiChatModel适配通义千问API
     */
    @Bean
    public ChatLanguageModel tongYiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeout))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建通义千问的EmbeddingModel
     * 用于RAG功能的文档向量化
     */
    @Bean
    public EmbeddingModel tongYiEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }
}