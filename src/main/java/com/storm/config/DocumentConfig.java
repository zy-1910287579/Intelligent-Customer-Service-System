package com.storm.config;

import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DocumentConfig {
    @Bean
    public TokenTextSplitter textSplitter() {



        /**
         * .withChunkSize(500) 不是“必须严格 500 tokens”，而是“目标大小约 500 tokens”。
         .withPunctuationMarks(...) 的作用就是：为了句子完整，允许 chunk 实际长度偏离 500 —— 可能 480，也可能 520，甚至 600。*/
        return TokenTextSplitter.builder()
                .withChunkSize(500)           //
                .withMinChunkSizeChars(150)   // 最小字符数，避免碎片
                .withPunctuationMarks(List.of('。', '？', '！', '；', '\n', '.', '?', '!'))
                .withKeepSeparator(true)
                .build();
    }

//    @Bean
//    public DocumentTransformer summaryEnricher(OpenAiChatModel openAiChatModel) {
//        return new SummaryMetadataEnricher(
//                openAiChatModel,
//                List.of(
//                        SummaryMetadataEnricher.SummaryType.PREVIOUS,
//                        SummaryMetadataEnricher.SummaryType.CURRENT,
//                        SummaryMetadataEnricher.SummaryType.NEXT
//                )
//        );
//    }
}
