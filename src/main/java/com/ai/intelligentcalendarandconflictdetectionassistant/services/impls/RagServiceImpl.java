package com.ai.intelligentcalendarandconflictdetectionassistant.services.impls;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG服务类，处理文档嵌入和查询功能
 */
@Service
@AllArgsConstructor
public class RagServiceImpl {

    private final VectorStore vectorStore;

    /**
     * 嵌入文件到向量数据库
     *
     * @param file 待嵌入的文件
     * @return 是否成功
     */
    @SneakyThrows
    public Boolean embedding(MultipartFile file) {
        // 从IO流中读取文件
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
        // 将文本内容划分成更小的块
        TextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(tikaDocumentReader.read());
        // 存入向量数据库，这个过程会自动调用embeddingModel,将文本变成向量再存入。
        vectorStore.add(splitDocuments);
        return true;
    }

    /**
     * 查询向量数据库
     *
     * @param query 用户的提问
     * @return 匹配到的文档
     */
    public List<Document> query(String query) {
        return vectorStore.similaritySearch(
            org.springframework.ai.vectorstore.SearchRequest.query(query)
        );
    }

    /**
     * 查询向量数据库，带相似度阈值
     *
     * @param query 用户的提问
     * @param similarityThreshold 相似度阈值
     * @return 匹配到的文档
     */
    public List<Document> query(String query, double similarityThreshold) {
        return vectorStore.similaritySearch(
            org.springframework.ai.vectorstore.SearchRequest.query(query)
                .withSimilarityThreshold(similarityThreshold)
        );
    }

    /**
     * 查询向量数据库，带相似度阈值和返回数量限制
     *
     * @param query 用户的提问
     * @param similarityThreshold 相似度阈值
     * @param topK 返回的文档数量
     * @return 匹配到的文档
     */
    public List<Document> query(String query, double similarityThreshold, int topK) {
        return vectorStore.similaritySearch(
            org.springframework.ai.vectorstore.SearchRequest.query(query)
                .withSimilarityThreshold(similarityThreshold)
                .withTopK(topK)
        );
    }
}