package com.microservices.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import com.microservices.notification_service.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

	@KafkaListener(topics = "notificationTopic")
	public void handleNotification(OrderPlacedEvent orderPlacedEvent) {
		log.info("Received Notification for Order - {}", orderPlacedEvent.getOrderNumber());
		
	}

}
