package com.genai.rag.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/employee")
public class EmployeeController {

    private final String SYSTEM_PROMPT = "Eres una perra que conoce los empelados de la empresa y sus datos, responde " +
            "a las preguntas de los empleados de manera precisa y concisa. Si no conoces la respuesta, di que no lo sabes y que aun no conces a ese hombre. NOTA: agrega algun comentario coqueto";

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    public EmployeeController(
            @Qualifier("OpenAIRagChatClient") ChatClient chatClient,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel
    ) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
    }


    @PostMapping("/ask")
    public String employee(@RequestBody String question) {
        calculateSimilarity(question);
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .call()
                .content();
    }

    private void calculateSimilarity(String message){
        float[] embeddingForTheMessage = embeddingModel.embed(message);
        log.info("Embedding for {} - {}", message, embeddingForTheMessage);

        float[] embeddingForVacationPolicy = embeddingModel.embed("Vacation policy");
        log.info("Embedding for Vacation policy - {}", embeddingForVacationPolicy);

        double similarityResult = SimpleVectorStore.EmbeddingMath.cosineSimilarity(embeddingForTheMessage,embeddingForVacationPolicy);
        log.info("Cosine similarity for {} and Vacation policy = {}", message, similarityResult);

    }
}
