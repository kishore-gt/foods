package com.srFoodDelivery.service.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.order.MultiOrderCreateRequest;
import com.srFoodDelivery.dto.order.MultiOrderDTO;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.PreorderSlot;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.SubOrderItem;
import com.srFoodDelivery.model.TableReservation;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.PreorderSlotRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.repository.TableReservationRepository;
import com.srFoodDelivery.service.MenuItemService;

@Service
@Transactional
public class OrderOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderOrchestrationService.class);

    private final MultiOrderRepository multiOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final MenuItemRepository menuItemRepository;
    private final PreorderSlotRepository preorderSlotRepository;
    private final TableReservationRepository tableReservationRepository;
    private final MenuItemService menuItemService;

    public OrderOrchestrationService(
            MultiOrderRepository multiOrderRepository,
            SubOrderRepository subOrderRepository,
            MenuItemRepository menuItemRepository,
            PreorderSlotRepository preorderSlotRepository,
            TableReservationRepository tableReservationRepository,
            MenuItemService menuItemService) {
        this.multiOrderRepository = multiOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.menuItemRepository = menuItemRepository;
        this.preorderSlotRepository = preorderSlotRepository;
        this.tableReservationRepository = tableReservationRepository;
        this.menuItemService = menuItemService;
    }

    /**
     * Creates a MultiOrder with SubOrders grouped by restaurant/chef.
     * This is a transactional operation - all or nothing.
     */
    public MultiOrderDTO createMultiOrder(User user, MultiOrderCreateRequest request) {
        logger.info("Creating MultiOrder for user: {}", user.getEmail());

        // Validate all menu items exist and are available
        Map<Long, MenuItem> menuItemMap = new HashMap<>();
        for (MultiOrderCreateRequest.CartItemRequest cartItem : request.getCartItems()) {
            MenuItem menuItem = menuItemRepository.findById(cartItem.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Menu item not found: " + cartItem.getMenuItemId()));

            if (!menuItem.isAvailable()) {
                throw new IllegalStateException(
                        "Menu item is not available: " + menuItem.getName());
            }

            if (cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive for item: " + menuItem.getName());
            }

            menuItemMap.put(cartItem.getMenuItemId(), menuItem);
        }

        // Group items by restaurant (chef feature removed - only restaurants supported)
        Map<Restaurant, List<MultiOrderCreateRequest.CartItemRequest>> restaurantGroups = new HashMap<>();

        for (MultiOrderCreateRequest.CartItemRequest cartItem : request.getCartItems()) {
            MenuItem menuItem = menuItemMap.get(cartItem.getMenuItemId());

            if (menuItem.getMenu().getRestaurant() != null) {
                Restaurant restaurant = menuItem.getMenu().getRestaurant();
                restaurantGroups.computeIfAbsent(restaurant, k -> new ArrayList<>()).add(cartItem);
            } else {
                throw new IllegalStateException(
                        "Menu item must belong to a restaurant: " + menuItem.getName());
            }
        }

        // Create MultiOrder
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setUser(user);
        multiOrder.setDeliveryAddress(request.getDeliveryAddress());
        multiOrder.setSpecialInstructions(request.getSpecialInstructions());
        multiOrder.setStatus("PENDING");
        multiOrder.setPaymentStatus("PENDING");
        multiOrder.setDiscountAmount(
                request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO);
        multiOrder.setAppliedCoupon(request.getAppliedCoupon());

        // Set default ordering mode to DELIVERY (will be overridden if
        // preorder/reservation)
        multiOrder.setOrderingMode("DELIVERY");

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Create SubOrders for restaurants (one per restaurant)
        for (Map.Entry<Restaurant, List<MultiOrderCreateRequest.CartItemRequest>> entry : restaurantGroups.entrySet()) {
            Restaurant restaurant = entry.getKey();
            List<MultiOrderCreateRequest.CartItemRequest> items = entry.getValue();

            SubOrder subOrder = createSubOrder(multiOrder, restaurant, items, menuItemMap);
            multiOrder.addSubOrder(subOrder);
            totalAmount = totalAmount.add(subOrder.getTotalAmount());
        }

        // Apply discount
        BigDecimal finalAmount = totalAmount.subtract(multiOrder.getDiscountAmount());
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        multiOrder.setTotalAmount(finalAmount);

        // Handle preorder slot if specified
        if (request.getPreorderSlotId() != null && !restaurantGroups.isEmpty()) {
            // Set ordering mode to PREORDER
            multiOrder.setOrderingMode("PREORDER");
            multiOrder.setStatus("PENDING_APPROVAL"); // Set status to PENDING_APPROVAL for owner review

            // For simplicity, assign to first restaurant suborder
            // In production, this should be more sophisticated (match by restaurant)
            Restaurant firstRestaurant = restaurantGroups.keySet().iterator().next();
            SubOrder preorderSubOrder = multiOrder.getSubOrders().stream()
                    .filter(so -> so.getRestaurant() != null && so.getRestaurant().equals(firstRestaurant))
                    .findFirst()
                    .orElse(null);

            if (preorderSubOrder != null) {
                PreorderSlot slot = preorderSlotRepository.findById(request.getPreorderSlotId())
                        .orElseThrow(() -> new IllegalArgumentException("Preorder slot not found"));

                // Reserve the slot atomically (this will fail if capacity is exhausted)
                // Note: This should be done before saving to ensure atomicity
                int rowsAffected = preorderSlotRepository.reserveSlotAtomic(slot.getId());
                if (rowsAffected == 0) {
                    throw new IllegalStateException("Preorder slot is fully booked. Please select another time slot.");
                }

                preorderSubOrder.setPreorderSlot(slot);
                preorderSubOrder.setOrderType("DINE_IN");
                preorderSubOrder.setStatus("PENDING_APPROVAL"); // Also set suborder status

                // Set scheduled delivery time from slot
                if (slot.getSlotStartTime() != null) {
                    multiOrder.setScheduledDeliveryTime(slot.getSlotStartTime());
                }
            }
        }

        // Handle table reservation if specified
        if (request.getReservationId() != null && !restaurantGroups.isEmpty()) {
            // Set ordering mode to DINE_IN if not already set to PREORDER
            if (!"PREORDER".equals(multiOrder.getOrderingMode())) {
                multiOrder.setOrderingMode("DINE_IN");
                multiOrder.setStatus("PENDING_APPROVAL"); // Set status to PENDING_APPROVAL for owner review
            }

            Restaurant firstRestaurant = restaurantGroups.keySet().iterator().next();
            SubOrder reservationSubOrder = multiOrder.getSubOrders().stream()
                    .filter(so -> so.getRestaurant() != null && so.getRestaurant().equals(firstRestaurant))
                    .findFirst()
                    .orElse(null);

            if (reservationSubOrder != null) {
                TableReservation reservation = tableReservationRepository.findById(request.getReservationId())
                        .orElseThrow(() -> new IllegalArgumentException("Table reservation not found"));

                // Link reservation and table to suborder
                reservationSubOrder.setReservation(reservation);
                reservationSubOrder.setTable(reservation.getTable());
                reservationSubOrder.setOrderType("DINE_IN");
                reservationSubOrder.setStatus("PENDING_APPROVAL"); // Also set suborder status

                // Update reservation status to CONFIRMED
                reservation.setStatus("CONFIRMED");
                tableReservationRepository.save(reservation);

                logger.info("Table reservation {} linked to SubOrder {}", reservation.getId(),
                        reservationSubOrder.getId());
            }
        }

        // Save MultiOrder (cascade saves SubOrders and SubOrderItems)
        MultiOrder savedMultiOrder = multiOrderRepository.save(multiOrder);
        logger.info("Created MultiOrder {} with {} SubOrders", savedMultiOrder.getId(),
                savedMultiOrder.getSubOrders().size());

        return convertToDTO(savedMultiOrder);
    }

    private SubOrder createSubOrder(
            MultiOrder multiOrder,
            Restaurant restaurant,
            List<MultiOrderCreateRequest.CartItemRequest> cartItems,
            Map<Long, MenuItem> menuItemMap) {

        SubOrder subOrder = new SubOrder();
        subOrder.setMultiOrder(multiOrder);
        subOrder.setRestaurant(restaurant);
        subOrder.setChefProfile(null); // Chef feature removed
        subOrder.setStatus("PENDING");
        // Default orderType to DELIVERY (will be overridden if preorder/reservation)
        subOrder.setOrderType("DELIVERY");

        BigDecimal subOrderTotal = BigDecimal.ZERO;

        for (MultiOrderCreateRequest.CartItemRequest cartItem : cartItems) {
            MenuItem menuItem = menuItemMap.get(cartItem.getMenuItemId());
            BigDecimal unitPrice = menuItem.getEffectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            SubOrderItem subOrderItem = new SubOrderItem();
            subOrderItem.setSubOrder(subOrder);
            subOrderItem.setMenuItem(menuItem);
            subOrderItem.setItemName(menuItem.getName());
            subOrderItem.setQuantity(cartItem.getQuantity());
            subOrderItem.setUnitPrice(unitPrice);
            subOrderItem.setLineTotal(lineTotal);

            subOrder.addItem(subOrderItem);
            subOrderTotal = subOrderTotal.add(lineTotal);
        }

        subOrder.setTotalAmount(subOrderTotal);
        return subOrder;
    }

    @Transactional(readOnly = true)
    public MultiOrderDTO getMultiOrderById(Long id, User user) {
        MultiOrder multiOrder = multiOrderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("MultiOrder not found"));
        return convertToDTO(multiOrder);
    }

    @Transactional(readOnly = true)
    public List<MultiOrderDTO> getUserMultiOrders(User user) {
        return multiOrderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private MultiOrderDTO convertToDTO(MultiOrder multiOrder) {
        MultiOrderDTO dto = new MultiOrderDTO();
        dto.setId(multiOrder.getId());
        dto.setUserId(multiOrder.getUser().getId());
        dto.setUserName(multiOrder.getUser().getFullName());
        dto.setTotalAmount(multiOrder.getTotalAmount());
        dto.setDiscountAmount(multiOrder.getDiscountAmount());
        dto.setAppliedCoupon(multiOrder.getAppliedCoupon());
        dto.setStatus(multiOrder.getStatus());
        dto.setDeliveryAddress(multiOrder.getDeliveryAddress());
        dto.setSpecialInstructions(multiOrder.getSpecialInstructions());
        dto.setPaymentStatus(multiOrder.getPaymentStatus());
        dto.setCreatedAt(multiOrder.getCreatedAt());
        dto.setUpdatedAt(multiOrder.getUpdatedAt());

        List<MultiOrderDTO.SubOrderDTO> subOrderDTOs = new ArrayList<>();
        for (SubOrder subOrder : multiOrder.getSubOrders()) {
            MultiOrderDTO.SubOrderDTO subOrderDTO = new MultiOrderDTO.SubOrderDTO();
            subOrderDTO.setId(subOrder.getId());

            if (subOrder.getRestaurant() != null) {
                subOrderDTO.setRestaurantId(subOrder.getRestaurant().getId());
                subOrderDTO.setRestaurantName(subOrder.getRestaurant().getName());
                // Include restaurant coordinates for map display
                if (subOrder.getRestaurant().getLatitude() != null &&
                        subOrder.getRestaurant().getLongitude() != null) {
                    subOrderDTO.setRestaurantLat(subOrder.getRestaurant().getLatitude().toString());
                    subOrderDTO.setRestaurantLon(subOrder.getRestaurant().getLongitude().toString());
                }
            }

            subOrderDTO.setStatus(subOrder.getStatus());
            subOrderDTO.setTotalAmount(subOrder.getTotalAmount());

            if (subOrder.getRider() != null) {
                subOrderDTO.setRiderId(subOrder.getRider().getId());
                if (subOrder.getRider().getUser() != null) {
                    subOrderDTO.setRiderName(subOrder.getRider().getUser().getFullName());
                }
            }

            if (subOrder.getPreorderSlot() != null) {
                subOrderDTO.setPreorderSlotId(subOrder.getPreorderSlot().getId());
                subOrderDTO.setPreorderSlotStartTime(subOrder.getPreorderSlot().getSlotStartTime());
                subOrderDTO.setPreorderSlotEndTime(subOrder.getPreorderSlot().getSlotEndTime());
            }

            if (subOrder.getReservation() != null) {
                subOrderDTO.setReservationId(subOrder.getReservation().getId());
                subOrderDTO.setReservationDate(subOrder.getReservation().getReservationDate().toString());
                subOrderDTO.setReservationTime(subOrder.getReservation().getReservationTime().toString());
                subOrderDTO.setDurationMinutes(subOrder.getReservation().getDurationMinutes());
                subOrderDTO.setNumberOfGuests(subOrder.getReservation().getNumberOfGuests());
            }

            if (subOrder.getTable() != null) {
                subOrderDTO.setTableId(subOrder.getTable().getId());
                subOrderDTO.setTableName(subOrder.getTable().getTableName());
                subOrderDTO.setTableNumber(subOrder.getTable().getTableNumber());
            }

            subOrderDTO.setOrderType(subOrder.getOrderType());
            subOrderDTO.setEstimatedDeliveryTime(subOrder.getEstimatedDeliveryTime());
            subOrderDTO.setActualDeliveryTime(subOrder.getActualDeliveryTime());
            subOrderDTO.setTrackingInfo(subOrder.getTrackingInfo());

            List<MultiOrderDTO.SubOrderItemDTO> itemDTOs = new ArrayList<>();
            for (SubOrderItem item : subOrder.getItems()) {
                MultiOrderDTO.SubOrderItemDTO itemDTO = new MultiOrderDTO.SubOrderItemDTO();
                itemDTO.setId(item.getId());
                if (item.getMenuItem() != null) {
                    itemDTO.setMenuItemId(item.getMenuItem().getId());
                }
                itemDTO.setItemName(item.getItemName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setLineTotal(item.getLineTotal());
                itemDTOs.add(itemDTO);
            }
            subOrderDTO.setItems(itemDTOs);
            subOrderDTOs.add(subOrderDTO);
        }
        dto.setSubOrders(subOrderDTOs);

        return dto;
    }
}
