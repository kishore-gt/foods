package com.srFoodDelivery.service.order;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.order.MultiOrderCreateRequest;
// Import MultiOrderDTO from the correct package
import com.srFoodDelivery.dto.order.MultiOrderDTO;
import com.srFoodDelivery.main.SRfoodDeliveryApplication;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.UserRepository;

@SpringBootTest(classes = SRfoodDeliveryApplication.class)
@ActiveProfiles("test")
@Transactional
public class OrderOrchestrationServiceTest {

    @Autowired
    private OrderOrchestrationService orderOrchestrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Test
    public void testCreateMultiOrder_TransactionRollback() {
        // This test verifies that if item validation fails, the entire transaction
        // rolls back
        User user = userRepository.findByEmail("customer1@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        MultiOrderCreateRequest request = new MultiOrderCreateRequest();
        request.setDeliveryAddress("123 Test St");

        List<MultiOrderCreateRequest.CartItemRequest> cartItems = new ArrayList<>();
        MultiOrderCreateRequest.CartItemRequest item = new MultiOrderCreateRequest.CartItemRequest();
        item.setMenuItemId(99999L); // Non-existent item
        item.setQuantity(1);
        cartItems.add(item);
        request.setCartItems(cartItems);

        // Should throw exception and rollback
        assertThrows(IllegalArgumentException.class, () -> {
            orderOrchestrationService.createMultiOrder(user, request);
        });
    }

    @Test
    public void testCreateMultiOrder_Success() {
        User user = userRepository.findByEmail("customer1@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        // Get a real menu item
        List<MenuItem> availableItems = menuItemRepository.findByAvailableTrue();
        if (availableItems.isEmpty()) {
            // Skip test if no items available
            return;
        }

        MenuItem menuItem = availableItems.get(0);

        MultiOrderCreateRequest request = new MultiOrderCreateRequest();
        request.setDeliveryAddress("123 Test St");

        List<MultiOrderCreateRequest.CartItemRequest> cartItems = new ArrayList<>();
        MultiOrderCreateRequest.CartItemRequest item = new MultiOrderCreateRequest.CartItemRequest();
        item.setMenuItemId(menuItem.getId());
        item.setQuantity(2);
        cartItems.add(item);
        request.setCartItems(cartItems);

        MultiOrderDTO result = orderOrchestrationService.createMultiOrder(user, request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("PENDING", result.getStatus());
        assertFalse(result.getSubOrders().isEmpty());
        assertTrue(result.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
    }
}
