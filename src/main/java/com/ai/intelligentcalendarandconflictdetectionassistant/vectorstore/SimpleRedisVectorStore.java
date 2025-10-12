package com.ai.intelligentcalendarandconflictdetectionassistant.vectorstore;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于普通Redis的向量数据库实现（不使用Redis Stack）
 * 使用Redis Hash存储文档和向量，实现简单的相似度搜索
 */
@Slf4j
public class SimpleRedisVectorStore implements VectorStore {

    private final EmbeddingModel embeddingModel;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String prefix;
    
    public SimpleRedisVectorStore(EmbeddingModel embeddingModel, 
                                 RedisConnectionFactory connectionFactory, 
                                 String prefix) {
        this.embeddingModel = embeddingModel;
        this.prefix = prefix != null ? prefix : "doc:";
        
        // 创建RedisTemplate
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(connectionFactory);
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        this.redisTemplate.afterPropertiesSet();
    }

    @Override
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        
        for (Document document : documents) {
            String documentId = prefix + UUID.randomUUID().toString();
            
            // 生成文档的向量
            float[] embeddingArray = embeddingModel.embed(document.getContent());
            List<Double> embedding = new ArrayList<>();
            for (float f : embeddingArray) {
                embedding.add((double) f);
            }
            
            // 存储文档信息到Redis Hash
            Map<String, String> documentData = new HashMap<>();
            documentData.put("id", documentId);
            documentData.put("content", document.getContent());
            documentData.put("metadata", document.getMetadata().toString());
            documentData.put("embedding", embeddingToString(embedding));
            
            redisTemplate.opsForHash().putAll(documentId, documentData);
            
            log.debug("Document stored in Redis: {}", documentId);
        }
    }



    @Override
    public Optional<Boolean> delete(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return Optional.of(true);
        }
        
        for (String id : idList) {
            redisTemplate.delete(prefix + id);
        }
        
        return Optional.of(true);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (request == null || request.getQuery() == null) {
            return Collections.emptyList();
        }
        
        // 生成查询向量的嵌入
        float[] queryEmbeddingArray = embeddingModel.embed(request.getQuery());
        List<Double> queryEmbedding = new ArrayList<>();
        for (float f : queryEmbeddingArray) {
            queryEmbedding.add((double) f);
        }
        
        // 获取所有文档键
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 计算每个文档与查询的相似度
        List<DocumentWithScore> scoredDocuments = new ArrayList<>();
        
        for (String key : keys) {
            Map<Object, Object> documentData = redisTemplate.opsForHash().entries(key);
            
            if (documentData.containsKey("embedding")) {
                String embeddingStr = (String) documentData.get("embedding");
                List<Double> documentEmbedding = stringToEmbedding(embeddingStr);
                
                // 计算余弦相似度
                double similarity = cosineSimilarity(queryEmbedding, documentEmbedding);
                
                // 应用相似度阈值过滤
                Double similarityThreshold = request.getSimilarityThreshold();
                double threshold = similarityThreshold != null ? 
                    similarityThreshold : 0.0;
                    
                if (similarity >= threshold) {
                    String content = (String) documentData.get("content");
                    String metadataStr = (String) documentData.get("metadata");
                    
                    Document document = new Document(content);
                    // 解析metadata字符串为Map（简化实现）
                    document.getMetadata().putAll(parseMetadata(metadataStr));
                    
                    scoredDocuments.add(new DocumentWithScore(document, similarity));
                }
            }
        }
        
        // 按相似度排序
        scoredDocuments.sort((d1, d2) -> Double.compare(d2.score, d1.score));
        
        // 应用topK限制
        Integer topKValue = request.getTopK();
        int topK = topKValue != null ? topKValue : 4;
        if (scoredDocuments.size() > topK) {
            scoredDocuments = scoredDocuments.subList(0, topK);
        }
        
        return scoredDocuments.stream()
                .map(docWithScore -> docWithScore.document)
                .collect(Collectors.toList());
    }

    /**
     * 将嵌入向量转换为字符串存储
     */
    private String embeddingToString(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * 将字符串解析为嵌入向量
     */
    private List<Double> stringToEmbedding(String embeddingStr) {
        return Arrays.stream(embeddingStr.split(","))
                .map(Double::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 解析metadata字符串（简化实现）
     */
    private Map<String, Object> parseMetadata(String metadataStr) {
        Map<String, Object> metadata = new HashMap<>();
        // 这里可以添加更复杂的解析逻辑
        metadata.put("source", "redis");
        return metadata;
    }

    /**
     * 内部类：文档和相似度分数
     */
    private static class DocumentWithScore {
        Document document;
        double score;
        
        DocumentWithScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }
}