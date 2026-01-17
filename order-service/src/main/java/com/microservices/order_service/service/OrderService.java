package com.microservices.order_service.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import com.microservices.order_service.model.Order;
import com.microservices.order_service.model.OrderLineItems;
import com.microservices.order_service.dto.OrderRequest;
import com.microservices.order_service.dto.OrderLineItemsDto;
import com.microservices.order_service.repository.OrderRepository;
import org.springframework.web.reactive.function.client.WebClient;
import com.microservices.order_service.dto.InventoryResponse;
import java.util.Arrays;
import org.springframework.kafka.core.KafkaTemplate;
import com.microservices.order_service.event.OrderPlacedEvent;
import java.time.Duration;
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    public String createOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
            .map(OrderLineItems::getSkuCode)
            .toList();

        // Configure WebClient with timeout (must be <= TimeLimiter timeout)
        // This ensures the WebClient call actually times out, not just the CompletableFuture wrapper
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
            .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
            .retrieve()
            .bodyToMono(InventoryResponse[].class)
            .timeout(Duration.ofSeconds(2)) // 2 seconds (less than TimeLimiter's 3s to ensure proper timeout)
            .doOnError(error -> log.error("WebClient timeout or error: {}", error.getMessage()))
            .block();
        
        log.info("Inventory responses: {}", Arrays.toString(inventoryResponses));
        // Critical: Check if inventory response is null or empty
        // This prevents orders from being placed when inventory service times out
        if (inventoryResponses == null || inventoryResponses.length == 0) {
            log.error("Inventory service returned null or empty response. Cannot verify stock availability.");
            throw new IllegalStateException("Unable to verify product availability. Please try again later.");
        }
        
        // Verify we have responses for all requested SKUs
        if (inventoryResponses.length != skuCodes.size()) {
            log.warn("Inventory response count ({}) does not match requested SKU count ({}). Some products may be missing.",
                    inventoryResponses.length, skuCodes.size());
        }
        
        boolean allProductsInStock = Arrays.stream(inventoryResponses)
            .allMatch(InventoryResponse::isInStock);
        if (allProductsInStock) {
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            log.info("Order placed successfully with order number: {}", order.getOrderNumber());
            return "Order placed successfully";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
