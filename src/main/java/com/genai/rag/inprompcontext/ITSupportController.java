package com.genai.rag.inprompcontext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/it-support")
public class ITSupportController {

    private final ChatClient chatClient;

    public ITSupportController(@Qualifier("openAiChatClientWithMemory") ChatClient chatClient) {
        this.chatClient = chatClient;
    }


}
