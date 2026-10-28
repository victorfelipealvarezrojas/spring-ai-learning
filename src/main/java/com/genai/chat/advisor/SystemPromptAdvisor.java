package com.genai.chat.advisor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SystemPromptAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("SystemPromptAdvisor adviseCall called with request:: {}", chatClientRequest.prompt().getUserMessage().getText());
        chatClientRequest = updateSystemMessage(chatClientRequest);
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("SystemPromptAdvisor adviseStream called with request: {}", chatClientRequest.prompt().getUserMessage().getText());
        chatClientRequest = updateSystemMessage(chatClientRequest);
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "SystemPromptAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private ChatClientRequest updateSystemMessage(ChatClientRequest chatClientRequest) {
        List<Message> existingMessages = new ArrayList<>(chatClientRequest.prompt().getUserMessages());
        SystemMessage existingSystemMessage = chatClientRequest.prompt().getSystemMessage();
        existingMessages.add(new SystemMessage("Recuerda que eres un asistente útil que resume cualquier contenido dado. " +
                "Asegúrate de que el resumen sea conciso, informativo y capture los puntos clave. " +
                "Usa un tono amigable y cercano manteniendo la profesionalidad. " +
                "No respondas nada que no sea el resumen. Si la pregunta no es sobre resumir, " +
                "responde con 'Solo puedo ayudar con tareas de resumen' y le agregas unos cuantos garabatos " +
                "chilenos partiendo por el siguiente: maldito hijo de perra!. " + existingSystemMessage));

        chatClientRequest = chatClientRequest.mutate().prompt(Prompt.builder()
                        .messages(existingMessages)
                .build()).build();

        String updatedSystemMessage = chatClientRequest.prompt().getSystemMessage().getText();
        log.info("System message is updated to: {}", updatedSystemMessage);

        return chatClientRequest;
    }
}
