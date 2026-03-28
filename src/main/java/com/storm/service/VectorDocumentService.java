package com.storm.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jaxb.core.v2.TODO;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorDocumentService {

    /*
    TODO 实习被问好例子之一----------------------------2.3步为何运行速度如此之慢???,是什么问题? 怎么解决的?? 学到了什么?
     1.因为调用大模型次数很多,到底运行时间不合理增加
     切分  ：   本地计算 → 毫秒级
     关键词：  4 chunks × 1 次 = 4 次 LLM 调用,因为每一个chunk都需要调用大模型总结关键词,时间是chunk.size()
     摘要  ：   4 chunks × ~2.5 次 = 10 次 LLM 调用,同理,每个chunk还需要上下文,故时间是大约3*chunk.size()
     解决方法一:只保留步骤 1+2
     KeywordMetadataEnricher 对rag价值高（提升检索相关性、支持关键词过滤）
     SummaryMetadataEnricher 中低（当前摘要有用，prev/next 在预处理阶段几乎无用）	切耗时极高（2~3×N 次调用）
     并且将每块关键词改为一个!
     解决方法二:RetrievalAugmentationAdvisor使用,它提供了更灵活和强大的 RAG 实现--
     --如查询转换、文档检索、文档后处理、查询增强等）来构建定制化的 RAG 流程。
     */

    /*!!!!!!!!!!!!!!!!!!!!!!!!这里我将使用模型评估来检测rag功能!!!!!!!!!!!!!!!!!!!!!2026-3-25*/
    private final PgVectorStore vectorStore;
    private final DocumentTransformer  tokenTextSplitter;
    /**private final DocumentTransformer summaryEnricher;*/

    private final MyBatisDocumentService myBatisDocumentService;

    private final JdbcTemplate jdbcTemplate; // 注入 JdbcTemplate



    /**
     * 根据文档的物理主键 ID 列表，从向量库和元数据库中删除文档
     * @param documentIds 要删除的文档 ID 列表
     */
    public void removeDocumentsByIds(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            log.warn("删除文档请求的 ID 列表为空，操作终止。");
            return;
        }

        log.info("收到删除 {} 个文档的请求，开始执行...", documentIds.size());

        // 步骤 1: 从元数据库中删除记录
        // 这是原子操作，失败则不应影响向量库
        myBatisDocumentService.removeDocumentsByIds(documentIds);

        // 步骤 2: 从向量库中删除对应的向量
        // 注意：PgVectorStore.delete() 方法可能不存在或不推荐使用。
        // 更安全的做法是让向量库在查询时通过 filter 忽略这些 ID。
        // 如果必须物理删除，可能需要手动执行 SQL 或使用 JdbcTemplate。
        // 这里我们暂时只操作元数据，后续可以优化。

        log.info("删除文档请求处理完成。");
    }

    /**
     * 根据用户 ID 和会话 ID，从向量库和元数据库中删除该会话下的所有文档
     * 这是一个更符合业务逻辑的删除方法
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void removeDocumentsByUserAndSession(String userId, String sessionId) {
        if (userId == null || userId.isBlank() || sessionId == null || sessionId.isBlank()) {
            log.warn("删除文档请求的 userId 或 sessionId 为空，操作终止。");
            return;
        }

        log.info("收到删除用户 [{}] 在会话 [{}] 中所有文档的请求，开始执行...", userId, sessionId);

        // 委托给 MyBatis 服务来处理，它会先查 ID 再删除
        myBatisDocumentService.removeDocumentsByUserAndSession(userId, sessionId);

        log.info("删除用户 [{}] 在会话 [{}] 中所有文档的请求处理完成。");
    }

    public List<Document> loadAndSplit(String filePath,String originalFilename,String userId,String sessionId) {
        Resource resource = new FileSystemResource(filePath);
        //核心转换!,文件转document对象列表
        //这里有一个学习点,如果你点击去TikaDocumentReader源码,你会发现里面并,欸有实现read方法,而是
       //调用的是接口里的read方法,read方法的返回值是get方法,所以这里调用的是get方法
        //1.读取
        List<Document> rawDocs = new TikaDocumentReader(resource).read();
        /*
        public TikaDocumentReader(String resourceUrl) {
        this(resourceUrl, ExtractedTextFormatter.defaults());

        在这段代码中，this(resourceUrl, ExtractedTextFormatter.defaults());
        的作用是在一个构造方法中调用同一个类的另一个构造方法。

        }*/

        /*TODO:实习被问好例子之二,---------------------------------在对文档加元数据的时候遇到了官方文档描述不清晰的情况
        *  1.解决方案,对文档阅读一边,检查是否遗落了知识点
        *  2.看之前的初级项目-----了解到和Document类有关
        *  3.看Document源码并结合ai解答成功解决了问题!!!!!哦耶
        *  4.有多种解决方案,我选择了最合适的哪一个,先给所有文档加元数据再分块
        * */



        //2.切分
        List<Document> chunks = tokenTextSplitter.apply(rawDocs);
        List<Document> processedDocs = new ArrayList<>();

        // 遍历分块后列表，使用 mutate() 修改元数据
        //在分块后的列表里加元数据是最好的
        // --- 修改点：使用传统 for 循环，确保索引 i 准确递增 ---
        for (int i = 0; i < chunks.size(); i++) {
            Document document = chunks.get(i);

            // --- A. 获取块索引 ---
            // 直接用循环变量 i，这就是最准确的 0, 1, 2...
            String chunkIndex = String.valueOf(i);

            // --- B. 清洗文件名 ---
            // 把 "test.pdf" 变成 "test_pdf"，防止 ID 包含非法字符
            String safeFileName = originalFilename.replaceAll("[^a-zA-Z0-9]", "_");

            // --- C. 构造唯一主键 ID ---
            // 格式：sessionid_filename_chunkIndex
            // 例如：rag_001_manual_pdf_0
            String rawId = userId + "_" + sessionId + "_" + safeFileName + "_" + chunkIndex;

            // 将其转换为标准的 UUID (Type 3/5 风格)
            // 这会把 "rag_001_..." 这种长字符串压缩成一个标准的 32位 UUID
            UUID safeUuid = UUID.nameUUIDFromBytes(rawId.getBytes());
            String uniqueId = safeUuid.toString();

            // --- D. 构建最终文档 ---
            Document finalDoc = document.mutate()
                    .id(uniqueId) // 设置物理主键，防止覆盖
                    .metadata("user_id", userId)
                    .metadata("session_id", sessionId)
                    .metadata("file_name", originalFilename)
                    .metadata("source", "user_upload")
                    .metadata("processed_date", System.currentTimeMillis())
                    .metadata("chunk_index", chunkIndex) // 建议：把 chunkIndex 也存进 metadata，方便以后调试查看
                    .build();
            processedDocs.add(finalDoc);
        }

        log.info("✅ 处理完成: 文件 [{}], 用户 [{}], 窗口 [{}], 共切分 {} 块",
                originalFilename, userId, sessionId, processedDocs.size());


        return processedDocs;
        }
    public void ingestFileToVectorStore(String filePath,String originalFilename,String userId,String sessionId) {


        // ==========================================
        // 1. 【新增】去重检查：先查数据库有没有
        // ==========================================
        boolean exists = checkFileExists(userId, sessionId, originalFilename);
        if (exists) {
            log.info("⏭️ 文件已存在，跳过入库: userId={}, sessionId={}, file={}", userId, sessionId, originalFilename);
            return; // 直接结束，不执行后续操作
        }

        List<Document> documents = loadAndSplit(filePath,originalFilename,userId,sessionId);

        log.info("🚀 开始存入向量库，共 {} 块", documents.size());
        //TODO 这里不能写死数字
        addDocumentsInBatches(documents, 10); //
    }

    /**
     * 检查文件是否已存在的辅助方法
     */
    private boolean checkFileExists(String userId, String sessionId, String fileName) {
        // SQL: 只要查到有一条记录，就说明文件已经处理过了
        String sql = """
            SELECT COUNT(*) 
            FROM vector_store  -- 注意：这里填实际的 PgVectorStore 表名
            WHERE metadata->>'user_id' = ? 
              AND metadata->>'session_id' = ? 
              AND metadata->>'file_name' = ?
            """;

        // 查询数量
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, sessionId, fileName);

        boolean b = count != null && count > 0;

        if(b){
            log.info("该文档已经上传过了哦!本次没存入向量库");
        }
        return b;
    }

    /**
     * 分批添加文档到向量库（适配阿里云 embedding 批量限制）
     */
    private void addDocumentsInBatches(List<Document> documents, int batchSize) {
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch); // 每次 ≤10 个
            System.out.println("已入库批次: " + (i / batchSize + 1) + ", 文档数: " + batch.size());
        }
    }


}
