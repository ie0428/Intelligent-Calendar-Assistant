package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.RagServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档控制器，处理文档嵌入到向量数据库的功能
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin
@AllArgsConstructor
public class DocumentController {

    private final RagServiceImpl ragService;

    /**
     * 嵌入文件到向量数据库
     *
     * @param file 待嵌入的文件
     * @return 是否成功
     */
    @PostMapping("/embedding")
    public ResponseEntity<Boolean> embedding(@RequestParam("file") MultipartFile file) {
        try {
            Boolean result = ragService.embedding(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("文档嵌入失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(false);
        }
    }

    /**
     * 查询向量数据库
     *
     * @param query 用户的提问
     * @return 匹配到的文档内容
     */
    @GetMapping("/query")
    public ResponseEntity<String> query(@RequestParam("query") String query) {
        try {
            var documents = ragService.query(query, 0.7, 5);
            StringBuilder result = new StringBuilder();
            if (documents.isEmpty()) {
                result.append("未找到相关文档");
            } else {
                result.append("找到 ").append(documents.size()).append(" 个相关文档:\n\n");
                for (int i = 0; i < documents.size(); i++) {
                    var doc = documents.get(i);
                    result.append("文档 ").append(i + 1).append(":\n");
                    result.append(doc.getContent()).append("\n\n");
                }
            }
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            System.err.println("文档查询失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("查询失败: " + e.getMessage());
        }
    }
}