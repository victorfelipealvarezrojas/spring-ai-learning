package com.genai.chat.memory.dto;

public enum OrderStatus {
    CREATED("creado"),
    PROCESSING("en preparación"),
    SHIPPED("enviado"),
    DELIVERED("entregado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

