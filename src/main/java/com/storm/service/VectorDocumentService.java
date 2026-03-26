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
import org.springframework.stereotype.Service;
import java.util.List;
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

    public List<Document> loadAndSplit(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        //核心转换!,文件转document对象列表
        //这里有一个学习点,如果你点击去TikaDocumentReader源码,你会发现里面并,欸有实现read方法,而是
       //调用的是接口里的read方法,read方法的返回值是get方法,所以这里调用的是get方法
        List<Document> rawDocs = new TikaDocumentReader(resource).read();

        // 1. 切分
        List<Document> chunks = tokenTextSplitter.apply(rawDocs);

        log.info("1.切分成功,切分{}块",chunks.size());

        // 2. 加关键词
        /**chunks = keywordEnricher.apply(chunks);

        log.info("2.关键词添加成功!");*/


        // 3. 加摘要（注意：summaryEnricher 需要连续文档才能生成 prev/next）
        /**chunks = summaryEnricher.apply(chunks);

        log.info("3.摘要添加成功!");*/

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


}
