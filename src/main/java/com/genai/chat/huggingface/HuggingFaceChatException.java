package com.genai.chat.huggingface;

public class HuggingFaceChatException extends RuntimeException {
    public HuggingFaceChatException(String message) {
        super(message);
    }

    public HuggingFaceChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
