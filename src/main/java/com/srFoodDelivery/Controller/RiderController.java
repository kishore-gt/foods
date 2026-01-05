package com.srFoodDelivery.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Rider;
import com.srFoodDelivery.model.RiderOffer;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.RiderOfferRepository;
import com.srFoodDelivery.repository.RiderRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.EmailService;
import com.srFoodDelivery.service.rider.RiderService;
import com.srFoodDelivery.websocket.OrderWebSocketPublisher;

@Controller
@RequestMapping("/rider")
public class RiderController {

    private static final Logger logger = LoggerFactory.getLogger(RiderController.class);

    private final RiderService riderService;
    private final RiderRepository riderRepository;
    private final SubOrderRepository subOrderRepository;
    private final RiderOfferRepository riderOfferRepository;
    private final OrderWebSocketPublisher webSocketPublisher;
    private final EmailService emailService;
    private final MultiOrderRepository multiOrderRepository;

    public RiderController(
            RiderService riderService,
            RiderRepository riderRepository,
            SubOrderRepository subOrderRepository,
            RiderOfferRepository riderOfferRepository,
            OrderWebSocketPublisher webSocketPublisher,
            EmailService emailService,
            MultiOrderRepository multiOrderRepository) {
        this.riderService = riderService;
        this.riderRepository = riderRepository;
        this.subOrderRepository = subOrderRepository;
        this.riderOfferRepository = riderOfferRepository;
        this.webSocketPublisher = webSocketPublisher;
        this.emailService = emailService;
        this.multiOrderRepository = multiOrderRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        // Get pending offers for this rider (orders they can accept)
        List<RiderOffer> pendingOffers = riderOfferRepository.findPendingOffersForRider(
                rider, LocalDateTime.now());
        
        // Get ALL available orders (broadcasted to all riders) - NEW: Show all available orders
        List<SubOrder> availableOrders = new ArrayList<>();
        if (rider.getIsOnline()) {
            // Get all orders that are available (CONFIRMED/OFFERED, no rider assigned)
            availableOrders = subOrderRepository.findAvailableOrdersForRiders();
            
            // Filter out orders that this rider already has an offer for
            List<Long> riderOfferSubOrderIds = pendingOffers.stream()
                    .map(offer -> offer.getSubOrder() != null ? offer.getSubOrder().getId() : null)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            
            availableOrders = availableOrders.stream()
                    .filter(order -> !riderOfferSubOrderIds.contains(order.getId()))
                    .collect(Collectors.toList());
        }
        
        // Get active orders - includes:
        // 1. Orders offered to this rider (OFFERED status with pending offer)
        // 2. Orders accepted by this rider (ACCEPTED, EN_ROUTE, PICKED_UP, OUT_FOR_DELIVERY)
        List<SubOrder> activeOrders = new ArrayList<>();
        
        // Add orders with pending offers for this rider
        for (RiderOffer offer : pendingOffers) {
            if (offer.getSubOrder() != null && "OFFERED".equals(offer.getSubOrder().getStatus())) {
                activeOrders.add(offer.getSubOrder());
            }
        }
        
        // Add orders accepted by this rider
        List<SubOrder> acceptedOrders = subOrderRepository
                .findByRiderAndStatusInOrderByCreatedAtDesc(
                        rider, 
                        Arrays.asList("ACCEPTED", "EN_ROUTE", "PICKED_UP", "OUT_FOR_DELIVERY"))
                .stream()
                .limit(10)
                .collect(Collectors.toList());
        
        activeOrders.addAll(acceptedOrders);
        
        // Remove duplicates and sort by creation date (most recent first)
        activeOrders = activeOrders.stream()
                .distinct()
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
                        return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                    }
                    return 0;
                })
                .limit(10)
                .collect(Collectors.toList());
        
        // Get today's completed orders
        LocalDate today = LocalDate.now();
        List<SubOrder> todayOrders = subOrderRepository
                .findByRiderAndStatusInOrderByCreatedAtDesc(
                        rider, 
                        List.of("DELIVERED", "COMPLETED"))
                .stream()
                .filter(order -> order.getUpdatedAt() != null && 
                        order.getUpdatedAt().toLocalDate().equals(today))
                .collect(Collectors.toList());
        
        // Calculate today's earnings (assuming ₹20 per delivery)
        BigDecimal todayEarnings = BigDecimal.valueOf(todayOrders.size() * 20);
        
        // Calculate total completed orders
        long totalCompleted = subOrderRepository
                .findByRiderAndStatusInOrderByCreatedAtDesc(
                        rider, 
                        List.of("DELIVERED", "COMPLETED"))
                .size();
        
        // Create a map of subOrderId -> offerId for easy lookup in template
        Map<Long, Long> orderOfferMap = new HashMap<>();
        for (RiderOffer offer : pendingOffers) {
            if (offer.getSubOrder() != null) {
                orderOfferMap.put(offer.getSubOrder().getId(), offer.getId());
            }
        }
        
        model.addAttribute("rider", rider);
        model.addAttribute("pendingOffers", pendingOffers); // Pending offers for this rider
        model.addAttribute("availableOrders", availableOrders); // NEW: All available orders (all riders can see)
        model.addAttribute("activeOrders", activeOrders); // Orders accepted by this rider
        model.addAttribute("orderOfferMap", orderOfferMap); // Map for offer ID lookup
        model.addAttribute("todayOrders", todayOrders);
        model.addAttribute("todayEarnings", todayEarnings);
        model.addAttribute("totalCompleted", totalCompleted);
        model.addAttribute("isOnline", rider.getIsOnline());
        
        return "rider/dashboard";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        List<SubOrder> allOrders = subOrderRepository
                .findByRiderOrderByCreatedAtDesc(rider);
        
        model.addAttribute("rider", rider);
        model.addAttribute("orders", allOrders);
        
        return "rider/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        SubOrder order = subOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // Verify order belongs to rider
        if (order.getRider() == null || !order.getRider().getId().equals(rider.getId())) {
            throw new SecurityException("Access denied");
        }
        
        model.addAttribute("rider", rider);
        model.addAttribute("order", order);
        
        return "rider/order-detail";
    }

    @PostMapping("/toggle-online")
    public String toggleOnline(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        boolean newStatus = !rider.getIsOnline();
        
        // If going online, location is required
        if (newStatus && (latitude == null || longitude == null)) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Please allow location access to go online");
            return "redirect:/rider/dashboard";
        }
        
        riderService.toggleOnlineStatus(rider.getId(), newStatus);
        
        // Update location if going online
        if (newStatus && latitude != null && longitude != null) {
            riderService.updateRiderLocation(rider.getId(), 
                BigDecimal.valueOf(latitude), 
                BigDecimal.valueOf(longitude));
        }
        
        redirectAttributes.addFlashAttribute(
                "successMessage", 
                newStatus ? "You are now ONLINE" : "You are now OFFLINE");
        
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/offers/{offerId}/accept")
    public String acceptOffer(
            @PathVariable Long offerId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        boolean accepted = riderService.handleRiderOfferResponseByOfferId(rider.getId(), offerId, true);
        
        if (accepted) {
            redirectAttributes.addFlashAttribute("successMessage", "Order offer accepted! Tracking has started.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept offer");
        }
        
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/offers/{offerId}/reject")
    public String rejectOffer(
            @PathVariable Long offerId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        riderService.handleRiderOfferResponseByOfferId(rider.getId(), offerId, false);
        redirectAttributes.addFlashAttribute("infoMessage", "Order offer rejected");
        
        return "redirect:/rider/dashboard";
    }

    /**
     * Accept an available order directly (NEW: For orders broadcasted to all riders)
     * Creates an offer and immediately accepts it atomically
     */
    @PostMapping("/orders/{subOrderId}/accept-available")
    public String acceptAvailableOrder(
            @PathVariable Long subOrderId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        if (!rider.getIsOnline()) {
            redirectAttributes.addFlashAttribute("errorMessage", "You must be online to accept orders");
            return "redirect:/rider/dashboard";
        }
        
        // Use pessimistic lock to check order availability atomically
        SubOrder order = subOrderRepository.findByIdWithLock(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // Check if order is available (with lock to prevent race condition)
        if (order.getRider() != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "This order has already been accepted by another rider");
            return "redirect:/rider/dashboard";
        }
        
        if (!"CONFIRMED".equals(order.getStatus()) && !"OFFERED".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "This order is not available for acceptance");
            return "redirect:/rider/dashboard";
        }
        
        // Ensure order status is OFFERED (for consistency)
        if ("CONFIRMED".equals(order.getStatus())) {
            order.setStatus("OFFERED");
            subOrderRepository.save(order);
        }
        
        // Check if rider already has an offer for this order
        Optional<RiderOffer> existingOffer = riderOfferRepository.findBySubOrderAndRiderAndStatus(
                order, rider, "PENDING");
        
        if (existingOffer.isPresent()) {
            // Accept existing offer (this will use pessimistic locking internally)
            boolean accepted = riderService.handleRiderOfferResponseByOfferId(
                    rider.getId(), existingOffer.get().getId(), true);
            if (accepted) {
                redirectAttributes.addFlashAttribute("successMessage", "Order accepted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept order (may have been taken)");
            }
        } else {
            // Create offer and accept immediately (atomic)
            try {
                // Create offer first
                RiderOffer offer = new RiderOffer();
                offer.setSubOrder(order);
                offer.setRider(rider);
                offer.setStatus("PENDING");
                offer.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // 30 min expiry
                RiderOffer savedOffer = riderOfferRepository.save(offer);
                
                // Immediately accept it (atomic operation with pessimistic locking)
                boolean accepted = riderService.handleRiderOfferResponseByOfferId(
                        rider.getId(), savedOffer.getId(), true);
                
                if (accepted) {
                    redirectAttributes.addFlashAttribute("successMessage", "Order accepted successfully!");
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept order (may have been taken by another rider)");
                }
            } catch (Exception e) {
                logger.error("Error accepting available order {} by rider {}: {}", 
                        subOrderId, rider.getId(), e.getMessage(), e);
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept order: " + e.getMessage());
            }
        }
        
        return "redirect:/rider/dashboard";
    }

    // Legacy endpoints for backward compatibility
    @PostMapping("/orders/{id}/accept")
    public String acceptOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        // Try to find offer first
        SubOrder order = subOrderRepository.findById(id).orElse(null);
        if (order != null) {
            // Use legacy method which will handle both old and new flows
            boolean accepted = riderService.handleRiderOfferResponse(rider.getId(), id, true);
            
            if (accepted && order.getMultiOrder() != null) {
                // Send WebSocket notification
                webSocketPublisher.publishOrderUpdate(
                        order.getMultiOrder().getId(),
                        id,
                        "ACCEPTED",
                        rider.getId(),
                        rider.getCurrentLatitude() != null ? rider.getCurrentLatitude().doubleValue() : null,
                        rider.getCurrentLongitude() != null ? rider.getCurrentLongitude().doubleValue() : null);
            }
            
            if (accepted) {
                redirectAttributes.addFlashAttribute("successMessage", "Order accepted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept order");
            }
        }
        
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/orders/{id}/decline")
    public String declineOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        riderService.handleRiderOfferResponse(rider.getId(), id, false);
        redirectAttributes.addFlashAttribute("infoMessage", "Order declined");
        
        return "redirect:/rider/dashboard";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        SubOrder order = subOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        if (order.getRider() == null || !order.getRider().getId().equals(rider.getId())) {
            throw new SecurityException("Access denied");
        }
        
        // Validate status transition - riders can only update to DELIVERED
        if (!"DELIVERED".equals(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Riders can only update order status to DELIVERED");
            return "redirect:/rider/orders/" + id;
        }
        
        // Only allow updating to DELIVERED if order is OUT_FOR_DELIVERY
        // Owner must update to OUT_FOR_DELIVERY first before rider can mark as delivered
        if (!"OUT_FOR_DELIVERY".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Order must be marked as 'Out for Delivery' by restaurant owner before you can mark it as delivered. Current status: " + order.getStatus());
            return "redirect:/rider/orders/" + id;
        }
        
        order.setStatus("DELIVERED");
        order.setActualDeliveryTime(LocalDateTime.now());
        subOrderRepository.save(order);

        // Update MultiOrder status if needed
        MultiOrder multiOrder = order.getMultiOrder();
        if (multiOrder != null) {
            // Update MultiOrder status to DELIVERED
            multiOrder.setStatus("DELIVERED");
            multiOrderRepository.save(multiOrder);
            
            // Send WebSocket notification
            webSocketPublisher.publishOrderUpdate(
                    multiOrder.getId(),
                    id,
                    status,
                    rider.getId(),
                    rider.getCurrentLatitude() != null ? rider.getCurrentLatitude().doubleValue() : null,
                    rider.getCurrentLongitude() != null ? rider.getCurrentLongitude().doubleValue() : null);
            
            // Send email notification to customer
            if (multiOrder.getUser() != null) {
                try {
                    emailService.sendMultiOrderStatusUpdateEmail(multiOrder.getUser(), multiOrder);
                } catch (Exception e) {
                    System.err.println("Failed to send delivery email: " + e.getMessage());
                }
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", "Order status updated!");

        return "redirect:/rider/orders/" + id;
    }

    @GetMapping("/earnings")
    public String earnings(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        // Get all completed orders
        List<SubOrder> completedOrders = subOrderRepository
                .findByRiderAndStatusInOrderByCreatedAtDesc(
                        rider, 
                        List.of("DELIVERED", "COMPLETED"));
        
        // Calculate earnings (₹20 per delivery)
        BigDecimal totalEarnings = BigDecimal.valueOf(completedOrders.size() * 20);
        
        // Group by date
        Map<LocalDate, List<SubOrder>> ordersByDate = completedOrders.stream()
                .filter(order -> order.getUpdatedAt() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getUpdatedAt().toLocalDate(),
                        Collectors.toList()));
        
        model.addAttribute("rider", rider);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("ordersByDate", ordersByDate);
        
        return "rider/earnings";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        model.addAttribute("rider", rider);
        model.addAttribute("user", user);
        
        return "rider/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String vehicleNumber,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Rider rider = riderService.getOrCreateRider(user);
        
        if (vehicleType != null) {
            rider.setVehicleType(vehicleType);
        }
        if (vehicleNumber != null) {
            rider.setVehicleNumber(vehicleNumber);
        }
        
        riderRepository.save(rider);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        
        return "redirect:/rider/profile";
    }

    private User requireUser(CustomUserDetails principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return principal.getUser();
    }
}

