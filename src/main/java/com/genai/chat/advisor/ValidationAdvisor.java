package com.genai.chat.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class ValidationAdvisor implements CallAdvisor, StreamAdvisor {
    private static final int MAX_TOKEN_INPUT = 5000;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        varidRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        validResponse(chatClientResponse);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private void varidRequest(ChatClientRequest chatClientRequest) {
        String input = chatClientRequest.prompt().getContents();
        if (input.length() > MAX_TOKEN_INPUT) {
            throw new IllegalArgumentException("Input is too long " + MAX_TOKEN_INPUT);
        }
    }

    private void validResponse(ChatClientResponse chatClientResponse) {
        String output = chatClientResponse.chatResponse().getResult().getOutput().getText();
        if (output == null || output.trim().isEmpty()) {
            throw new IllegalArgumentException("Output is empty");
        }
    }


}
