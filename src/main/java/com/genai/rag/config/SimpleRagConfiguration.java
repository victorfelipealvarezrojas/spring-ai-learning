package com.genai.rag.config;

import com.genai.rag.config.data.RagConfigData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class SimpleRagConfiguration {

    private final static String FILE_PATH = "classpath:rag/employee/files/*.md";
    private final static String VECTOR_STORE_PATH = "src/main/resources/rag/employee/vector-store/vectorstore.json";


    /**
     * Splitter que divide los Document ya extraídos (por un DocumentReader) en chunks
     * para generar embeddings. No participa en la extracción del texto; opera en la
     * etapa de transformación del pipeline ETL.
     *
     * - chunkSize: tamaño objetivo de cada chunk, en tokens (encoding CL100K_BASE).
     * - minChunkSizeChars: mínimo de caracteres antes de intentar cortar en el último
     *   signo de puntuación (. ? ! \n). Si no se alcanza, el chunk se deja completo.
     * - minChunkLengthToEmbed: chunks con menos caracteres que este valor (tras trim)
     *   se descartan y no se embeben.
     * - maxNumChunks: máximo de chunks por documento; el texto restante se descarta.
     * - keepSeparator: true conserva los saltos de línea; false los reemplaza por espacios.
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter(RagConfigData ragConfigData){
        return TokenTextSplitter.builder()
                .withChunkSize(ragConfigData.getChunk().getSize())
                .withMinChunkSizeChars(ragConfigData.getChunk().getMinChunkSize())
                .withMinChunkLengthToEmbed(ragConfigData.getChunk().getMinChunkEmbed())
                .withMaxNumChunks(ragConfigData.getChunk().getMaxNumChunks())
                .withKeepSeparator(ragConfigData.getChunk().isKeepSeparator())
                .build();
    }


    /**
     * VectorStore en memoria (SimpleVectorStore) que usa el ChatClient para la recuperación en RAG.
     * Calcula la similitud por coseno sobre los embeddings que mantiene en memoria y puede
     * persistirlos en un archivo JSON.
     *
     * Flujo:
     * - Si existe el archivo en VECTOR_STORE_PATH y forceRebuild es false, carga los embeddings
     *   ya calculados desde disco. Así evita volver a llamar al modelo de embeddings.
     * - En caso contrario, ejecuta la ingesta completa:
     *   1. loadDocuments(): extracción del texto (DocumentReader).
     *   2. getChunks(): división en chunks con el TokenTextSplitter.
     *   3. add(): genera el embedding de cada chunk con el EmbeddingModel de OpenAI. Esta llamada tiene costo.
     *   4. save(): persiste el resultado en disco para los próximos arranques.
     *
     * @param embeddingModel    modelo de embeddings de OpenAI, usado tanto en la ingesta como en las consultas
     * @param tokenTextSplitter splitter que define el tamaño y el corte de los chunks
     * @param ragConfigData     configuración de RAG; forceRebuild obliga a reconstruir el store
     */
    @Bean
    public SimpleVectorStore simpleVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
                                               TokenTextSplitter tokenTextSplitter,
                                               RagConfigData ragConfigData
    ) throws IOException {

        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        var vectorStoreFile = new File(VECTOR_STORE_PATH);

        if (vectorStoreFile.exists() && !ragConfigData.isForceRebuild()) {
            log.info("Loading vector store from file: {}", VECTOR_STORE_PATH);
            simpleVectorStore.load(vectorStoreFile);
            return simpleVectorStore;
        }

        // ingest & build vector store
        log.info("Create a new vector store from files: {}", VECTOR_STORE_PATH);
        var documents = loadDocuments();
        var chunks = getChunks(documents, tokenTextSplitter);
        simpleVectorStore.add(chunks);
        simpleVectorStore.save(vectorStoreFile);
        return simpleVectorStore;

    }

    private List<Document> getChunks(List<Document> documents, TokenTextSplitter tokenTextSplitter) {
        return tokenTextSplitter.apply(documents);
    }

    private List<Document> loadDocuments() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(FILE_PATH);
        List<Document> documents = new ArrayList<>();
        for (Resource resource: resources){
            TextReader reader = new TextReader(resource);
            reader.getCustomMetadata()
                    .put("category", Objects.requireNonNull(resource.getFilename()).replace(".md", ""));
            reader.getCustomMetadata().put("access_level", "public");
            reader.getCustomMetadata().put("version", "2026.01");
            documents.addAll(reader.read());
        }
        return documents;
    }

}
