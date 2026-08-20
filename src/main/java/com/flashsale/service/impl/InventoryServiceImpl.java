package com.flashsale.service.impl;

import com.flashsale.entity.Inventory;
import com.flashsale.entity.Product;
import com.flashsale.repository.InventoryRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.InventoryService;
import com.flashsale.exception.InsufficientStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Inventory createInventory(Long productId, Integer quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Inventory inventory = Inventory.builder()
                .product(product)
                .availableQuantity(quantity)
                .reservedQuantity(0)
                .build();

        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory getInventory(Long productId) {

        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
    }

    @Override
    public Inventory addStock(Long productId, Integer quantity) {

        Inventory inventory = getInventory(productId);

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + quantity
        );

        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory reserveStock(Long productId, Integer quantity) {

        Inventory inventory = getInventory(productId);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock");
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + quantity
        );

        return inventoryRepository.save(inventory);
    }
}