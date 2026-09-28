package com.genai.config;

import com.genai.chat.advisor.ErrorWrappingAdvisor;
import com.genai.chat.advisor.SystemPromptAdvisor;
import com.genai.chat.advisor.ValidationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
