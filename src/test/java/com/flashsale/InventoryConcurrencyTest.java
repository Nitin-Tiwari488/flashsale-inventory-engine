package com.flashsale;

import com.flashsale.entity.Inventory;
import com.flashsale.repository.InventoryRepository;
import com.flashsale.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void testConcurrentReservation() throws Exception {

        Long productId = 1L;

        // Reset inventory to 10
        Inventory inventory = inventoryService.getInventory(productId);
        inventory.setAvailableQuantity(10);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        int numberOfRequests = 50;

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        CountDownLatch doneSignal =
                new CountDownLatch(numberOfRequests);

        AtomicInteger successfulReservations =
                new AtomicInteger(0);

        AtomicInteger failedReservations =
                new AtomicInteger(0);

        for (int i = 0; i < numberOfRequests; i++) {

            executor.submit(() -> {

                try {
                    startSignal.await();

                    inventoryService.reserveStock(productId, 1);

                    successfulReservations.incrementAndGet();

                } catch (Exception e) {

                    failedReservations.incrementAndGet();

                    System.out.println(
                            "Reservation failed: "
                                    + e.getClass().getSimpleName()
                    );


                } finally  {
                    doneSignal.countDown();
                }
            });
        }

        // Start all threads together
        startSignal.countDown();

        // Wait for all requests
        doneSignal.await();

        executor.shutdown();

        Inventory finalInventory =
                inventoryService.getInventory(productId);

        System.out.println("================================");
        System.out.println("Concurrent requests: " + numberOfRequests);
        System.out.println(
                "Successful reservations: "
                        + successfulReservations.get()
        );
        System.out.println(
                "Available stock: "
                        + finalInventory.getAvailableQuantity()
        );
        System.out.println(
                "Reserved stock: "
                        + finalInventory.getReservedQuantity()
        );

        System.out.println(
                "Invariant: " +
                        (finalInventory.getAvailableQuantity()
                                + finalInventory.getReservedQuantity())
        );
        System.out.println(
                "Failed reservations: "
                        + failedReservations.get()
        );
        System.out.println("================================");
    }
    @Test
    void testConcurrentReservationPessimistic() throws Exception {

        Long productId = 1L;

        // Reset inventory
        Inventory inventory = inventoryService.getInventory(productId);
        inventory.setAvailableQuantity(10);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        int numberOfRequests = 50;

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        CountDownLatch doneSignal =
                new CountDownLatch(numberOfRequests);

        AtomicInteger successfulReservations =
                new AtomicInteger(0);

        AtomicInteger failedReservations =
                new AtomicInteger(0);

        for (int i = 0; i < numberOfRequests; i++) {

            executor.submit(() -> {

                try {
                    startSignal.await();

                    inventoryService.reserveStockPessimistic(
                            productId, 1
                    );

                    successfulReservations.incrementAndGet();

                } catch (Exception e) {

                    failedReservations.incrementAndGet();

                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Start all requests
        startSignal.countDown();

        // Wait for completion
        doneSignal.await();

        executor.shutdown();

        Inventory finalInventory =
                inventoryService.getInventory(productId);

        System.out.println("================================");
        System.out.println("PESSIMISTIC LOCKING");
        System.out.println("Concurrent requests: " + numberOfRequests);
        System.out.println(
                "Successful reservations: "
                        + successfulReservations.get()
        );
        System.out.println(
                "Failed reservations: "
                        + failedReservations.get()
        );
        System.out.println(
                "Available stock: "
                        + finalInventory.getAvailableQuantity()
        );
        System.out.println(
                "Reserved stock: "
                        + finalInventory.getReservedQuantity()
        );
        System.out.println(
                "Invariant: "
                        + (finalInventory.getAvailableQuantity()
                        + finalInventory.getReservedQuantity())
        );
        System.out.println("================================");
    }
}