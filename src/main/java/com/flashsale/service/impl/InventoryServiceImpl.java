package com.flashsale.service.impl;

import com.flashsale.entity.Inventory;
import com.flashsale.entity.Product;
import com.flashsale.repository.InventoryRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.InventoryService;
import com.flashsale.exception.InsufficientStockException;
import com.flashsale.service.RedisInventoryService;
import com.flashsale.service.InventoryEventProducer;
import com.flashsale.event.InventoryReservationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flashsale.service.MetricsService;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    private final RedisInventoryService redisInventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    private final MetricsService metricsService;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            RedisInventoryService redisInventoryService,
            InventoryEventProducer inventoryEventProducer,
            MetricsService metricsService) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.redisInventoryService = redisInventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
        this.metricsService = metricsService;
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

        if (inventory.getAvailableQuantity() < quantity) {
            metricsService.reservationFailure();
            throw new InsufficientStockException("Insufficient stock");
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + quantity
        );

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        metricsService.reservationSuccess();

        return savedInventory;
    }

    @Transactional
    @Override
    public Inventory reserveStockPessimistic(
            Long productId,
            Integer quantity) {

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() ->
                        new RuntimeException("Inventory not found"));

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

    @Override
    public Inventory reserveStockRedis(
            Long productId,
            Integer quantity) {

        boolean reserved = redisInventoryService
                .reserveStock(productId, quantity);

        if (!reserved) {
            throw new InsufficientStockException(
                    "Insufficient stock in Redis"
            );
        }

        inventoryEventProducer.publishReservation(
                new InventoryReservationEvent(productId, quantity)
        );

        return getInventory(productId);
    }

    @Override
    @Transactional
    public Inventory releaseStock(
            Long productId,
            Integer quantity) {

        Inventory inventory = getInventory(productId);

        if (inventory.getReservedQuantity() < quantity) {
            throw new RuntimeException(
                    "Cannot release more stock than reserved"
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - quantity
        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + quantity
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        redisInventoryService.releaseStock(productId, quantity);

        return savedInventory;
    }
    @Override
    @Transactional
    public Inventory resetStock(Long productId, Integer quantity) {

        Inventory inventory = getInventory(productId);

        inventory.setAvailableQuantity(quantity);
        inventory.setReservedQuantity(0);

        return inventoryRepository.save(inventory);
    }
}

