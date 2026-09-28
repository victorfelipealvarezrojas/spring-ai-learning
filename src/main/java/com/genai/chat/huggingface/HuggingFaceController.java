package com.genai.chat.huggingface;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/huggingface/chat")
public class HuggingFaceController {

    private final HuggingFaceService huggingFaceService;

    public HuggingFaceController(HuggingFaceService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
    }

    @PostMapping("/summarize")
    public String summarize(@RequestBody String message) {
        return this.huggingFaceService.chat(message);
    }
}
