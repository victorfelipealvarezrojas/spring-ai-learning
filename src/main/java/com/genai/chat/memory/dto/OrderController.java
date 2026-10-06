package com.genai.chat.memory.dto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/orders/chats")
public class OrderController {
    private final ChatClient chatClient;
    private final OrderStatusTools orderStatusTools;
    private final ChatMemory chatMemory;

    private static final String SYSTEM_PROMPT = """
            Eres un asistente virtual de seguimiento de pedidos.
            
            Tu única tarea es informar al usuario el estado de envío de sus pedidos.
            
            Reglas:
            - Para conocer el estado de un pedido, usa siempre la herramienta getOrderStatus. \
              Nunca inventes ni supongas un estado.
            
            - Si el usuario no indica un ID de pedido, usa el último ID mencionado en la \
            conversación. Si no hay ninguno, pídeselo.
            
            - Si la herramienta indica que el pedido no existe o no pertenece al usuario, \
              infórmalo sin dar más detalles.
            
            - Responde de forma breve: ID del pedido, transportista si está disponible, y estado.
            - Si la consulta no trata sobre pedidos, responde que solo puedes ayudar con \
              el seguimiento de pedidos.
              
            NOTA: AGERGA "mi estimada putita" al finalizar la respuesta
            """;

    public OrderController(
            @Qualifier("openAiChatClientWithMemory") ChatClient chatClient,
            OrderStatusTools orderStatusTools,
            ChatMemory chatMemory
    ) {
        this.chatClient = chatClient;
        this.orderStatusTools = orderStatusTools;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/status")
    public String OrderStatus(@RequestBody String message, @RequestHeader(value = "user-id") String userId) {
        log.info("Chat memory for current user Id: {}", userId);
        chatMemory.get(userId).forEach(m ->
                log.info("{} {}", m.getMessageType(), m.getMetadata()));

        String userMessage = message + ". user-id " + userId;
        return chatClient.prompt()
                .tools(orderStatusTools)
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("User Query: {userMessage}").param("userMessage", userMessage))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId)) // id por solicitud., en caso contrario todas las solicitudes comparten la misma memeoria
                .call()
                .content();


    }


}
