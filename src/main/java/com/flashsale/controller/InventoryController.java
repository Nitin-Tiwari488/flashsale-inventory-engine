package com.flashsale.controller;

import com.flashsale.entity.Inventory;
import com.flashsale.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Inventory> createInventory(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        return new ResponseEntity<>(
                inventoryService.createInventory(productId, quantity),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                inventoryService.getInventory(productId)
        );
    }

    @PatchMapping("/{productId}/add")
    public ResponseEntity<Inventory> addStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                inventoryService.addStock(productId, quantity)
        );
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Inventory> reserveStock(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                inventoryService.reserveStock(productId, quantity)
        );
    }
}