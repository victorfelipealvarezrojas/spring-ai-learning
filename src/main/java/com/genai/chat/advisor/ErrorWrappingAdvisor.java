package com.genai.chat.advisor;

import com.genai.chat.openai.dto.response.SummarizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
public class ErrorWrappingAdvisor implements CallAdvisor, StreamAdvisor {

    private final ObjectMapper objectMapper;

    public ErrorWrappingAdvisor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("ErrorWrappingAdvisor adviseCall called with request: {}", chatClientRequest.prompt().getUserMessage().getText());

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        assert chatClientResponse.chatResponse() != null;
        String assistantMessage = chatClientResponse.chatResponse().getResult().getOutput().getText().trim();

        if (!assistantMessage.startsWith("{") || !assistantMessage.endsWith("}")) {
            log.error("ErrorWrappingAdvisor: Assistant message is not valid JSON: {}", assistantMessage);
            SummarizationResponse summarizationResponse = new SummarizationResponse(
                    null,
                    null,
                    "Assistant message is not valid JSON XD: " + assistantMessage
            );

            try {
                chatClientResponse = chatClientResponse
                        .mutate()
                        .chatResponse(ChatResponse
                                .builder()
                                .generations(List.of(new Generation(new AssistantMessage(objectMapper.writeValueAsString(summarizationResponse)))))
                                .build())
                        .context(chatClientRequest.context())
                        .build();
            } catch (JacksonException e) {
                throw new RuntimeException(e);
            }

        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "ErrorWrappingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
