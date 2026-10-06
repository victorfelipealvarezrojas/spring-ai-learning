package com.genai.chat.googlegemini;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "gemini")
@RequestMapping("api/google/chat")
public class GoogleGeminiController {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "Eres un asistente útil que genera comentarios JavaDoc profesionales para código Java. " +
            "Explica siempre el propósito de la clase o método, sus parámetros, valores de retorno y cualquier excepción que pueda lanzar. " +
            "Usa el estilo estándar de JavaDoc con /** ... */. " +
            "Mantén las explicaciones concisas, claras y técnicas.";

    public GoogleGeminiController(@Qualifier("geminiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/generate-java-docs")
    public ChatClientResponse generateJavaDocs(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }
}
