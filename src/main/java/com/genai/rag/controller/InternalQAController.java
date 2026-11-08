package com.genai.rag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal-qa")
public class InternalQAController {

    private final ChatClient chatClient;
    private final String SYSTEM_PROMP = "eres un asistente para documentos internos: ofrece soluciones claras y concisas, " +
            "utiliza únicamente el contexto proporcionado y, si no sabes cómo responder, di que no lo sabes. " +
            "cite el recurso como [source: page_number]";

    public InternalQAController(@Qualifier("OpenAiAdvancedRagClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/ask")
    public String getInformation(@RequestBody String message,
                                 @RequestParam(value = "filter", required = false) String filter) {
        var prompt = chatClient.prompt()
                .system(SYSTEM_PROMP)
                .user(message);

        if (filter != null) {
            prompt = prompt.advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filter));
        }

        return prompt.call().content();
    }
}