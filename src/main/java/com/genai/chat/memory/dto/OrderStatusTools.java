package com.genai.chat.memory.dto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.genai.chat.memory.dto.OrderStatus.*;


@Slf4j
// es un singleton
@Component
public class OrderStatusTools {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initialize 10 orders
        orders.put("1", new Order("1", "FedEx", CREATED, "user1", "John Doe"));
        orders.put("2", new Order("2", "UPS", CREATED, "user2", "Jane Smith"));
        orders.put("3", new Order("3", "DHL", CREATED, "user3", "Alice Johnson"));
        orders.put("4", new Order("4", "USPS", CREATED, "user4", "Bob Brown"));
        orders.put("5", new Order("5", "FedEx", CREATED, "user5", "Charlie Wilson"));
        orders.put("6", new Order("6", "UPS", CREATED, "user6", "Diana Miller"));
        orders.put("7", new Order("7", "DHL", CREATED, "user7", "Eve Davis"));
        orders.put("8", new Order("8", "USPS", SHIPPED, "user8", "Frank Taylor"));
        orders.put("9", new Order("9", "FedEx", CREATED, "user9", "Grace Anderson"));
        orders.put("10", new Order("10", "UPS", CREATED, "user10", "Henry Thomas"));
    }

    @Tool
    public String getOrderStatus(
            @ToolParam(description = "Order Id") String orderId,
            @ToolParam(description = "User Id") String userId) {

        log.info("getOrderStatus orderId={} userId={}", orderId, userId);

        Order order = orders.get(orderId);
        if (order == null) {
            return "Order not found";
        }
        if (!order.userId().equals(userId)) {
            return "Order not found or user mismatch";
        }

        OrderStatus currentStatus = order.status();
        OrderStatus nexStatus = nexStage(currentStatus);

        Order updateOrder = new Order(
                order.orderId(),
                order.carrier(),
                nexStatus,
                order.userId(),
                order.userName()
        );
        orders.put(orderId, updateOrder);

        log.info("Order {} for user id {} status updated from {} to {}", orderId, userId, currentStatus, nexStatus);
        return "Order " + orderId + "for user id" + userId + " status updated to " + nexStatus;
    }

    private OrderStatus nexStage(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case CREATED -> PROCESSING;
            case PROCESSING -> SHIPPED;
            case SHIPPED -> DELIVERED;
            case DELIVERED -> DELIVERED;
        };
    }
}
