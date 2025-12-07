package com.microservices.inventory_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import com.microservices.inventory_service.repository.InventoryRepository;
import com.microservices.inventory_service.model.Inventory;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository) {
		return args -> {
			Inventory inventory = new Inventory();
			inventory.setSkuCode("iphone_13_red");
			inventory.setQuantity(100);
            inventoryRepository.save(inventory);

            inventory = new Inventory();
            inventory.setSkuCode("iphone_13_blue");
            inventory.setQuantity(0);
            inventoryRepository.save(inventory);
        };
    }

}
