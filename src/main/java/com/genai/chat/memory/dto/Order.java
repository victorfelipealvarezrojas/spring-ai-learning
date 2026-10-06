package com.genai.chat.memory.dto;

public record Order(
        String orderId,
        String carrier,
        OrderStatus status,
        String userId,
        String userName
) {
}
