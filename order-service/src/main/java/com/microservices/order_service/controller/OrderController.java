package com.microservices.order_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.microservices.order_service.service.OrderService;
import com.microservices.order_service.dto.OrderRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "inventory") // order is not matter according to my observations
    @Retry(name = "inventory", fallbackMethod = "fallbackMethod") // order is not matter according to my observations
    @TimeLimiter(name = "inventory")
    public CompletableFuture<String> createOrder(@RequestBody OrderRequest orderRequest) {
        return CompletableFuture.supplyAsync(() -> {
            return orderService.createOrder(orderRequest);
        });
    }
    public CompletableFuture<String> fallbackMethod(OrderRequest orderRequest, Exception exception) {
        if (exception instanceof TimeoutException) {
            log.warn("Fallback executed due to TimeoutException: {}", exception.getMessage());
            return CompletableFuture.supplyAsync(() -> "Request timed out! Please try again later.");
        } else {
            log.warn("Fallback executed due to {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
            return CompletableFuture.supplyAsync(() -> "Oops! Something went wrong, please order after some time!");
        }
    }
}
