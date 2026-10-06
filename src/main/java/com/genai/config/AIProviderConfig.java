package com.genai.config;

import com.genai.chat.advisor.ErrorWrappingAdvisor;
import com.genai.chat.advisor.SystemPromptAdvisor;

import com.genai.chat.advisor.ValidationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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
     *
     *  - initializeSchema: crea extensiones, tabla e índice si no existen (nunca los modifica).
     *  - indexType / distanceType: definen el índice HNSW con métrica coseno;
     *    la misma métrica se usa en las consultas de similitud.
     *  - dimensions: debe coincidir con spring.ai.openai.embedding.options.dimensions.
     *
     * Este bean reemplaza la autoconfiguración: cualquier propiedad
     * spring.ai.vectorstore.pgvector.* del YAML se ignora.
     * Es necesario porque hay más de un EmbeddingModel registrado (OpenAI y Google);
     * el @Qualifier fija el de OpenAI.
     */
    @Bean
    public PgVectorStore pgVectorStore(
            JdbcTemplate jdbcTemplate,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .dimensions(1024)
                .maxDocumentBatchSize(10_000)
                .build();
    }


    // creo un bean para ChatMemoryRepository asi usa la bd configurada por medio de las propiedades spring.datasource.*  en application.properties
    // la configuracion es automatica y la memoria se registra en una tabla default llamada spring_ai_chat_memory, si se desea cambiar el nombre de la tabla
    // se puede hacer con la propiedad spring.ai.chat.memory.table-name
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(5)
                .build();
    }

    @Bean()
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "gemini")
    public ChatClient geminiChatClient(GoogleGenAiChatModel googleGenAiChatModel) {
        return ChatClient.builder(googleGenAiChatModel).build();
    }

    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor(){
        return new SimpleLoggerAdvisor();
    }

    @Bean
    SafeGuardAdvisor safeGuardAdvisor(){
        return new SafeGuardAdvisor(List.of("potitio", "penesito"));
    }
}
