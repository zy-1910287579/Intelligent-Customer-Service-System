package com.storm.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorDocumentService {

    private final PgVectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    public List<Document> loadAndSplit(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        //核心转换!,文件转document对象列表
        List<Document> rawDocs = new TikaDocumentReader(resource).read();

        List<Document>chunks= tokenTextSplitter.apply(rawDocs);

        //打印分块
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i).getText();
            log.info("Chunk {} | Text: {}", i, content);
        }
        return  chunks;
    }
    public void ingestFileToVectorStore(String filePath) {
        List<Document> documents = loadAndSplit(filePath);
        //TODO 这里不能写死数字
        addDocumentsInBatches(documents, 10); //
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

//    public void clearKnowledgeBase() {
//        vectorStore.deleteAll(); // 删除所有向量记录
//    }
}
