package com.genai.config;

import com.genai.chat.advisor.ErrorWrappingAdvisor;
import com.genai.chat.advisor.SystemPromptAdvisor;

import com.genai.chat.advisor.ValidationAdvisor;
import com.genai.rag.config.data.PgVectorStoreConfigData;
import com.genai.rag.config.data.RagConfigData;
import com.genai.rag.preprocessor.DomainSynonymTransformer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Vector;

@Configuration
public class AIProviderConfig {

    @Bean()
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai", matchIfMissing = true)
    public ChatClient openAiChatClient(
            OpenAiChatModel openAiChatModel,
            SimpleLoggerAdvisor simpleLoggerAdvisor,
            SafeGuardAdvisor safeGuardAdvisor,
            ErrorWrappingAdvisor errorWrappingAdvisor,
            SystemPromptAdvisor systemPromptAdvisor,
            ValidationAdvisor validationAdvisor
    ) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(
                        simpleLoggerAdvisor,
                        safeGuardAdvisor,
                        errorWrappingAdvisor,
                        systemPromptAdvisor,
                        validationAdvisor
                )
                .build();
    }

    @Bean("openAiChatClientWithMemory")
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai", matchIfMissing = true)
    public ChatClient openAiChatClientWithMemory(
            OpenAiChatModel openAiChatModel,
            ChatMemory chatMemory,
            PgVectorStore pgVectorStore
    ) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(
                        VectorStoreChatMemoryAdvisor.builder(pgVectorStore)
                                // Debe ejecutarse antes que el ToolCallingAdvisor (order menor = más externo).
                                // Si queda dentro del bucle de herramientas, se vuelve a ejecutar tras cada
                                // llamada a una tool y busca en memoria con un input vacío.
                                .order(Ordered.HIGHEST_PRECEDENCE)
                                .build())
                .build();
    }


    /**
     * Vector store sobre Postgres (pgvector).
     * Actúa sobre la base definida en spring.datasource (postgresdb) mediante el JdbcTemplate.
     * <p>
     * - initializeSchema: crea extensiones, tabla e índice si no existen (nunca los modifica).
     * - indexType / distanceType: definen el índice HNSW con métrica coseno;
     * la misma métrica se usa en las consultas de similitud.
     * - dimensions: debe coincidir con spring.ai.openai.embedding.options.dimensions.
     * <p>
     * Este bean reemplaza la autoconfiguración: cualquier propiedad
     * spring.ai.vectorstore.pgvector.* del YAML se ignora.
     * Es necesario porque hay más de un EmbeddingModel registrado (OpenAI y Google);
     * el @Qualifier fija el de OpenAI.
     */
    @Bean("pgChatMemoryVectorStore")
    public PgVectorStore pgChatMemoryVectorStore(
            JdbcTemplate jdbcTemplate,
            PgVectorStoreConfigData pgVectorStoreConfigData,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForChatMemory())
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }

    @Primary
    @Bean("ragVectorStore")
    public VectorStore ragVectorStore(JdbcTemplate jdbcTemplate,
                                      PgVectorStoreConfigData pgVectorStoreConfigData,
                                      @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel
    ) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForRag())
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }


    @Bean
    RetrievalAugmentationAdvisor ragAdvisor(@Qualifier("ragVectorStore") VectorStore vectorStore,
                                            RagConfigData ragConfigData,
                                            DomainSynonymTransformer domainSynonymTransformer

    ){
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(domainSynonymTransformer)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(ragConfigData.getTopK())
                        .similarityThreshold(ragConfigData.getSimilarityThreshold())
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
                        .build())
                .build();

    }

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


    // creo un bean para ChatMemoryRepository asi usa la bd configurada por medio de las propiedades spring.datasource.*  en application.properties
    // la configuracion es automatica y la memoria se registra en una tabla default llamada spring_ai_chat_memory, si se desea cambiar el nombre de la tabla
    // se puede hacer con la propiedad spring.ai.chat.memory.table-name
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(5)
                .build();
    }


    @Bean("OpenAIRagChatClient")
    public ChatClient OpenAIRagChatClient(
            OpenAiChatModel openAiChatModel,
            SimpleVectorStore simpleVectorStore,
            SimpleLoggerAdvisor simpleLoggerAdvisor,
            RagConfigData ragConfigData
    ) {

        // QuestionAnswerAdvisor es RAG de una sola etapa, determinista. El RetrievalAugmentationAdvisor
        // es el mismo flujo separado en etapas, donde cada etapa que agregas puede introducir una llamada al modelo.

        return ChatClient
                .builder(openAiChatModel)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(simpleVectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(ragConfigData.getTopK())
                                .similarityThreshold(ragConfigData.getSimilarityThreshold())
                                .build())
                        .build(), simpleLoggerAdvisor)
                .build();
    }

    @Bean("OpenAiAdvancedRagClient")
    public ChatClient OpenAiAdvancedRagClient(OpenAiChatModel openAiChatModel,
                                              RetrievalAugmentationAdvisor retrievalAugmentationAdvisor
    ) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }

    @Bean
    @Primary
    EmbeddingModel primaryEmbedding(@Qualifier("openAiEmbeddingModel") EmbeddingModel delegateEmbeddingModel) {
        return delegateEmbeddingModel;
    }

    @Bean()
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "gemini")
    public ChatClient geminiChatClient(GoogleGenAiChatModel googleGenAiChatModel) {
        return ChatClient.builder(googleGenAiChatModel).build();
    }

    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    SafeGuardAdvisor safeGuardAdvisor() {
        return new SafeGuardAdvisor(List.of("potitio", "penesito"));
    }
}

