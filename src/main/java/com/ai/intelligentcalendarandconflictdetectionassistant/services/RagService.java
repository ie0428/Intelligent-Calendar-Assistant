package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG服务接口
 */
public interface RagService {
    
    /**
     * 嵌入文件到向量数据库
     * @param file 待嵌入的文件
     * @return 是否成功
     */
    Boolean embedding(MultipartFile file);
    
    /**
     * 查询向量数据库
     * @param query 用户的提问
     * @return 匹配到的文档
     */
    List<Document> query(String query);
    
    /**
     * 查询向量数据库，带相似度阈值
     * @param query 用户的提问
     * @param similarityThreshold 相似度阈值
     * @return 匹配到的文档
     */
    List<Document> query(String query, double similarityThreshold);
    
    /**
     * 查询向量数据库，带相似度阈值和返回数量限制
     * @param query 用户的提问
     * @param similarityThreshold 相似度阈值
     * @param topK 返回的文档数量
     * @return 匹配到的文档
     */
    List<Document> query(String query, double similarityThreshold, int topK);
}