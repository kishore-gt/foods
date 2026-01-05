package com.srFoodDelivery.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.srFoodDelivery.main.SRfoodDeliveryApplication;
import com.srFoodDelivery.model.PreorderSlot;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.repository.PreorderSlotRepository;
import com.srFoodDelivery.repository.RestaurantRepository;

@SpringBootTest(classes = SRfoodDeliveryApplication.class)
@ActiveProfiles("test")
public class PreorderServiceConcurrencyTest {

    @Autowired
    private PreorderService preorderService;

    @Autowired
    private PreorderSlotRepository preorderSlotRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Test
    public void testConcurrentSlotReservation() throws InterruptedException {
        // Create a test restaurant
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            // Skip if no restaurants
            return;
        }

        Restaurant restaurant = restaurants.get(0);

        // Create a slot with capacity 5
        PreorderSlot slot = new PreorderSlot();
        slot.setRestaurant(restaurant);
        slot.setSlotStartTime(LocalDateTime.now().plusHours(1));
        slot.setSlotEndTime(LocalDateTime.now().plusHours(2));
        slot.setMaxCapacity(5);
        slot.setCurrentCapacity(0);
        slot.setIsActive(true);
        PreorderSlot savedSlot = preorderSlotRepository.save(slot);

        int numberOfThreads = 10;
        int expectedSuccessfulReservations = 5; // Max capacity

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Try to reserve from multiple threads simultaneously
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    boolean reserved = preorderService.reserveSlot(savedSlot.getId());
                    if (reserved) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verify that exactly maxCapacity reservations succeeded
        assertEquals(expectedSuccessfulReservations, successCount.get(),
                "Expected exactly " + expectedSuccessfulReservations + " successful reservations");
        
        // Verify the slot's current capacity matches successful reservations
        PreorderSlot updatedSlot = preorderSlotRepository.findById(savedSlot.getId())
                .orElseThrow();
        assertEquals(expectedSuccessfulReservations, updatedSlot.getCurrentCapacity().intValue());
    }
}

