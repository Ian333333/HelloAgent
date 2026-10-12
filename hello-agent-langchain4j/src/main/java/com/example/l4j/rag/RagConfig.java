package com.example.l4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.List;

/**
 * RAG 核心配置：本地 embedding 模型 + 向量库 + 检索器 + 启动时摄取语料。
 *
 * 离线摄取流程（应用启动时执行一次）：
 * 1. 加载 classpath:docs/*.md 语料
 * 2. 递归切块（300 token / 50 token 重叠）
 * 3. bge-small-zh 本地向量化后写入内存向量库
 */
@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    private static final int CHUNK_SIZE = 300;
    private static final int CHUNK_OVERLAP = 50;

    /** 本地 ONNX 中文 embedding 模型（512 维，无需 API Key）。 */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new BgeSmallZhV15EmbeddingModel();
    }

    /** 内存向量库（演示用；生产可换 PGVector/Milvus 等，Bean 类型不变）。 */
    @Bean
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 知识检索器：向量检索 top-4。
     * minScore 基于实测校准（见 RagPipelineTest 的分数观察）：
     * bge-small-zh 对相关查询得分约 0.88，无关查询约 0.64，取 0.7 可过滤无关结果。
     */
    @Bean
    public ContentRetriever contentRetriever(InMemoryEmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(4)
                .minScore(0.7)
                .build();
    }

    /** 启动时摄取 classpath:docs 下的语料（切块 -> 向量化 -> 入库）。 */
    @Bean
    public ApplicationRunner ragIngestionRunner(InMemoryEmbeddingStore<TextSegment> embeddingStore,
                                                EmbeddingModel embeddingModel) {
        return args -> {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:docs/*.md");
            int totalSegments = 0;
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String filename = resource.getFilename();
                    Document document = new TextDocumentParser().parse(is);
                    document.metadata().put("source", filename);

                    List<TextSegment> segments =
                            DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP).split(document);
                    Response<List<Embedding>> embeddings = embeddingModel.embedAll(segments);
                    embeddingStore.addAll(embeddings.content(), segments);
                    totalSegments += segments.size();
                    log.info("RAG 语料摄取：{} -> {} 个切片", filename, segments.size());
                }
            }
            log.info("RAG 语料摄取完成：共 {} 个文档，{} 个切片入库", resources.length, totalSegments);
        };
    }
}
