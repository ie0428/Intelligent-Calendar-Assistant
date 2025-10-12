package com.ai.intelligentcalendarandconflictdetectionassistant.langchain4j;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
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
import java.util.stream.Collectors;

/**
 * 增强的RAG服务，结合LangChain4j的标准化组件
 */
@Service
@AllArgsConstructor
public class EnhancedRAGService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 嵌入文件到向量数据库（使用LangChain4j标准化组件）
     */
    @SneakyThrows
    public Boolean embeddingWithLangChain4j(MultipartFile file) {
        // 使用Spring AI读取文档
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new InputStreamResource(file.getInputStream()));
        TextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(tikaDocumentReader.read());
        
        // 转换为LangChain4j的TextSegment
        List<TextSegment> textSegments = splitDocuments.stream()
                .map(doc -> TextSegment.from(doc.getContent()))
                .collect(Collectors.toList());
        
        // 使用LangChain4j的标准化嵌入流程
        EmbeddingStoreIngestor.ingest((dev.langchain4j.data.document.Document) textSegments, embeddingStore);
        return true;
    }

    /**
     * 语义搜索（结合工具调用的上下文）
     */
    public List<String> semanticSearchWithContext(String query, String context, int maxResults) {
        // 结合查询和上下文进行更精准的搜索
        String enhancedQuery = query + " " + context;
        
        List<Document> documents = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.query(enhancedQuery)
                        .withTopK(maxResults)
                        .withSimilarityThreshold(0.6)
        );
        
        return documents.stream()
                .map(Document::getContent)
                .collect(Collectors.toList());
    }

    /**
     * 获取RAG增强的提示词
     */
    public String getRAGEnhancedPrompt(String userMessage, String toolContext) {
        // 基于工具上下文进行RAG检索
        List<String> relevantDocs = semanticSearchWithContext(userMessage, toolContext, 3);
        
        if (relevantDocs.isEmpty()) {
            return userMessage;
        }
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("基于以下知识库信息回答用户问题：\n\n");
        
        for (int i = 0; i < relevantDocs.size(); i++) {
            promptBuilder.append("【相关文档" + (i + 1) + "】: ").append(relevantDocs.get(i)).append("\n\n");
        }
        
        promptBuilder.append("用户问题：").append(userMessage);
        
        return promptBuilder.toString();
    }

    /**
     * 工具调用前的RAG预处理
     */
    public String preprocessForToolCalling(String userMessage, String toolName) {
        // 根据工具名称获取相关的上下文信息
        String toolContext = getToolContext(toolName);
        return getRAGEnhancedPrompt(userMessage, toolContext);
    }

    private String getToolContext(String toolName) {
        switch (toolName) {
            case "createBooking":
                return "日程创建 时间安排 冲突检测";
            case "getBookings":
                return "日程查询 时间范围 用户日程";
            case "cancelBooking":
                return "日程取消 状态变更 日程管理";
            case "changeBooking":
                return "日程修改 时间调整 内容更新";
            case "checkConflict":
                return "冲突检测 时间重叠 智能建议";
            default:
                return "日程管理 时间安排 智能助手";
        }
    }
}