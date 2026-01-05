package com.srFoodDelivery.service.rider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.Rider;
import com.srFoodDelivery.model.RiderOffer;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.RestaurantRiderRepository;
import com.srFoodDelivery.repository.RiderOfferRepository;
import com.srFoodDelivery.repository.RiderRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.websocket.OrderWebSocketPublisher;

@Service
@Transactional
public class RiderService {

    private static final Logger logger = LoggerFactory.getLogger(RiderService.class);
    private static final int MAX_ASSIGNMENT_ATTEMPTS = 3;

    public enum AssignmentStrategy {
        NEAREST,
        LEAST_LOADED
    }

    private final RiderRepository riderRepository;
    private final SubOrderRepository subOrderRepository;
    private final RestaurantRiderRepository restaurantRiderRepository;
    private final RiderOfferRepository riderOfferRepository;
    private final OrderWebSocketPublisher webSocketPublisher;
    private static final int OFFER_EXPIRY_MINUTES = 5; // Offers expire after 5 minutes

    public RiderService(
            RiderRepository riderRepository,
            SubOrderRepository subOrderRepository,
            RestaurantRiderRepository restaurantRiderRepository,
            RiderOfferRepository riderOfferRepository,
            OrderWebSocketPublisher webSocketPublisher) {
        this.riderRepository = riderRepository;
        this.subOrderRepository = subOrderRepository;
        this.restaurantRiderRepository = restaurantRiderRepository;
        this.riderOfferRepository = riderOfferRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    /**
     * Sends an offer to the nearest rider for a suborder (NEW FLOW: Offer before assignment)
     * @param excludeRiderIds Optional list of rider IDs to exclude (e.g., previously rejected riders)
     */
    public Optional<RiderOffer> sendOfferToNearestRider(Long subOrderId, AssignmentStrategy strategy, List<Long> excludeRiderIds) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));

        // Check if already has a rider assigned
        if (subOrder.getRider() != null) {
            throw new IllegalStateException("SubOrder already has a rider assigned");
        }

        // Check for existing pending offers (only if not excluding riders - i.e., first attempt)
        if (excludeRiderIds == null || excludeRiderIds.isEmpty()) {
        List<RiderOffer> existingOffers = riderOfferRepository.findPendingOffersForSubOrder(
                subOrder, LocalDateTime.now());
        if (!existingOffers.isEmpty()) {
            logger.warn("SubOrder {} already has pending offers", subOrderId);
            return Optional.of(existingOffers.get(0)); // Return first pending offer
            }
        }

        Restaurant restaurant = subOrder.getRestaurant();
        if (restaurant == null) {
            throw new IllegalStateException("SubOrder must have a restaurant for rider assignment");
        }

        List<Rider> candidates = getAvailableRidersForRestaurant(restaurant);
        
        // Exclude specified riders (e.g., previously rejected riders)
        if (excludeRiderIds != null && !excludeRiderIds.isEmpty()) {
            candidates = candidates.stream()
                    .filter(r -> !excludeRiderIds.contains(r.getId()))
                    .toList();
        }
        
        // Also exclude riders who have already rejected this order
        List<RiderOffer> previousRejections = riderOfferRepository.findBySubOrderAndStatus(
                subOrder, "REJECTED");
        List<Long> rejectedRiderIds = previousRejections.stream()
                .map(offer -> offer.getRider().getId())
                .toList();
        
        if (!rejectedRiderIds.isEmpty()) {
            candidates = candidates.stream()
                    .filter(r -> !rejectedRiderIds.contains(r.getId()))
                    .toList();
        }
        
        if (candidates.isEmpty()) {
            logger.warn("No available riders for restaurant {} after exclusions", restaurant.getId());
            return Optional.empty();
        }

        Rider selectedRider = null;

        switch (strategy) {
            case NEAREST:
                selectedRider = selectNearestRider(candidates, restaurant);
                break;
            case LEAST_LOADED:
                selectedRider = selectLeastLoadedRider(candidates);
                break;
            default:
                selectedRider = candidates.get(0); // Default to first available
        }

        if (selectedRider != null) {
            // Create offer instead of directly assigning
            RiderOffer offer = new RiderOffer();
            offer.setSubOrder(subOrder);
            offer.setRider(selectedRider);
            offer.setStatus("PENDING");
            offer.setExpiresAt(LocalDateTime.now().plusMinutes(OFFER_EXPIRY_MINUTES));
            
            RiderOffer savedOffer = riderOfferRepository.save(offer);
            
            // Update suborder status to OFFERED
            subOrder.setStatus("OFFERED");
            subOrderRepository.save(subOrder);
            
            logger.info("Sent offer to rider {} for SubOrder {} (expires in {} minutes)", 
                    selectedRider.getId(), subOrderId, OFFER_EXPIRY_MINUTES);
            
            // Send WebSocket notification to rider
            if (subOrder.getMultiOrder() != null) {
                Map<String, Object> notificationPayload = new HashMap<>();
                notificationPayload.put("offerId", savedOffer.getId());
                notificationPayload.put("subOrderId", subOrder.getId());
                notificationPayload.put("multiOrderId", subOrder.getMultiOrder().getId());
                notificationPayload.put("status", "OFFERED");
                notificationPayload.put("restaurantName", subOrder.getRestaurant() != null ? subOrder.getRestaurant().getName() : "Restaurant");
                notificationPayload.put("totalAmount", subOrder.getTotalAmount());
                notificationPayload.put("deliveryAddress", subOrder.getMultiOrder().getDeliveryAddress());
                notificationPayload.put("expiresAt", offer.getExpiresAt().toString());
                
                webSocketPublisher.publishToRider(selectedRider.getId(), "ORDER_OFFER", notificationPayload);
            }
            
            return Optional.of(savedOffer);
        }

        return Optional.empty();
    }
    
    /**
     * Sends an offer to the nearest rider for a suborder (overload without exclusions)
     */
    public Optional<RiderOffer> sendOfferToNearestRider(Long subOrderId, AssignmentStrategy strategy) {
        return sendOfferToNearestRider(subOrderId, strategy, null);
    }

    /**
     * Legacy method - now redirects to offer-based flow
     * @deprecated Use sendOfferToNearestRider instead
     */
    @Deprecated
    public Optional<Rider> assignRiderToSubOrder(Long subOrderId, AssignmentStrategy strategy) {
        Optional<RiderOffer> offer = sendOfferToNearestRider(subOrderId, strategy);
        return offer.map(RiderOffer::getRider);
    }

    /**
     * Auto-sends offer to nearest rider using NEAREST strategy
     */
    public Optional<RiderOffer> autoSendOffer(Long subOrderId) {
        return sendOfferToNearestRider(subOrderId, AssignmentStrategy.NEAREST);
    }

    /**
     * Broadcasts an order to ALL online riders (NEW: All riders can see and accept)
     * Creates offers for all online available riders
     */
    public List<RiderOffer> broadcastOrderToAllRiders(Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));

        // Check if already has a rider assigned
        if (subOrder.getRider() != null) {
            logger.warn("SubOrder {} already has a rider assigned, skipping broadcast", subOrderId);
            return new ArrayList<>();
        }

        // Check if order is already accepted or delivered
        if ("ACCEPTED".equals(subOrder.getStatus()) || 
            "EN_ROUTE".equals(subOrder.getStatus()) || 
            "DELIVERED".equals(subOrder.getStatus()) ||
            "COMPLETED".equals(subOrder.getStatus())) {
            logger.warn("SubOrder {} is already in status {}, skipping broadcast", subOrderId, subOrder.getStatus());
            return new ArrayList<>();
        }

        // Check if order is already in OFFERED status (already broadcasted)
        if ("OFFERED".equals(subOrder.getStatus())) {
            // Get existing offers
            List<RiderOffer> existingOffers = riderOfferRepository.findPendingOffersForSubOrder(
                    subOrder, LocalDateTime.now());
            if (!existingOffers.isEmpty()) {
                logger.info("SubOrder {} already broadcasted, returning existing offers", subOrderId);
                return existingOffers;
            }
            // If status is OFFERED but no pending offers, continue to create new offers
        }

        Restaurant restaurant = subOrder.getRestaurant();
        if (restaurant == null) {
            throw new IllegalStateException("SubOrder must have a restaurant for rider assignment");
        }

        // Get ALL online available riders (not just restaurant-specific)
        List<Rider> allOnlineRiders = riderRepository.findOnlineAvailableRidersWithLocation();
        
        if (allOnlineRiders.isEmpty()) {
            logger.warn("No online riders available for SubOrder {}", subOrderId);
            return new ArrayList<>();
        }

        // Create offers for ALL online riders
        List<RiderOffer> createdOffers = new ArrayList<>();
        subOrder.setStatus("OFFERED");
        subOrderRepository.save(subOrder);

        for (Rider rider : allOnlineRiders) {
            // Check if rider already has an offer for this suborder
            Optional<RiderOffer> existingOffer = riderOfferRepository.findBySubOrderAndRiderAndStatus(
                    subOrder, rider, "PENDING");
            
            if (existingOffer.isPresent()) {
                // Skip if offer already exists
                continue;
            }

            RiderOffer offer = new RiderOffer();
            offer.setSubOrder(subOrder);
            offer.setRider(rider);
            offer.setStatus("PENDING");
            offer.setExpiresAt(LocalDateTime.now().plusMinutes(OFFER_EXPIRY_MINUTES));
            
            RiderOffer savedOffer = riderOfferRepository.save(offer);
            createdOffers.add(savedOffer);
            
            // Send WebSocket notification to each rider
            if (subOrder.getMultiOrder() != null) {
                Map<String, Object> notificationPayload = new HashMap<>();
                notificationPayload.put("offerId", savedOffer.getId());
                notificationPayload.put("subOrderId", subOrder.getId());
                notificationPayload.put("multiOrderId", subOrder.getMultiOrder().getId());
                notificationPayload.put("status", "AVAILABLE");
                notificationPayload.put("restaurantName", restaurant.getName());
                notificationPayload.put("restaurantAddress", restaurant.getAddress());
                notificationPayload.put("totalAmount", subOrder.getTotalAmount());
                notificationPayload.put("deliveryAddress", subOrder.getMultiOrder().getDeliveryAddress());
                notificationPayload.put("expiresAt", offer.getExpiresAt().toString());
                notificationPayload.put("itemCount", subOrder.getItems() != null ? subOrder.getItems().size() : 0);
                
                webSocketPublisher.publishToRider(rider.getId(), "NEW_ORDER_AVAILABLE", notificationPayload);
            }
        }
        
        logger.info("Broadcasted SubOrder {} to {} riders", subOrderId, createdOffers.size());
        
        // Also publish a general notification that a new order is available
        if (subOrder.getMultiOrder() != null) {
            Map<String, Object> broadcastPayload = new HashMap<>();
            broadcastPayload.put("subOrderId", subOrder.getId());
            broadcastPayload.put("multiOrderId", subOrder.getMultiOrder().getId());
            broadcastPayload.put("restaurantName", restaurant.getName());
            broadcastPayload.put("totalAmount", subOrder.getTotalAmount());
            broadcastPayload.put("deliveryAddress", subOrder.getMultiOrder().getDeliveryAddress());
            
            // Publish to all riders topic (if you have one) or individual notifications are enough
        }
        
        return createdOffers;
    }

    /**
     * Legacy method - redirects to offer-based flow
     * @deprecated Use autoSendOffer instead
     */
    @Deprecated
    public Optional<Rider> autoAssignRider(Long subOrderId) {
        Optional<RiderOffer> offer = autoSendOffer(subOrderId);
        return offer.map(RiderOffer::getRider);
    }

    /**
     * Handles rider offer response using offerId (preferred method)
     * This is the main method that handles offer acceptance/rejection
     * Uses pessimistic locking to ensure atomic acceptance (only one rider can accept)
     */
    public boolean handleRiderOfferResponseByOfferId(Long riderId, Long offerId, boolean accept) {
        // Use pessimistic locking to ensure atomic acceptance
        RiderOffer offer = riderOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (!offer.getRider().getId().equals(riderId)) {
            throw new IllegalStateException("This offer does not belong to the rider");
        }

        if (!offer.isPending()) {
            throw new IllegalStateException("Offer is no longer pending");
        }

        SubOrder subOrder = offer.getSubOrder();
        
        // CRITICAL: Lock the suborder row to prevent concurrent acceptance
        // Use pessimistic lock to ensure only one rider can accept
        SubOrder lockedSubOrder = subOrderRepository.findByIdWithLock(subOrder.getId())
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));
        
        // Check again if already assigned (double-check after lock)
        if (lockedSubOrder.getRider() != null) {
            logger.warn("SubOrder {} already assigned to rider {}, rejecting acceptance by rider {}", 
                    lockedSubOrder.getId(), lockedSubOrder.getRider().getId(), riderId);
            // Reject this offer since order is already taken
            offer.setStatus("REJECTED");
            riderOfferRepository.save(offer);
            return false;
        }

        Rider rider = offer.getRider();

        if (accept) {
            // Accept the offer - assign rider and start tracking (ATOMIC OPERATION)
            offer.setStatus("ACCEPTED");
            riderOfferRepository.save(offer);
            
            // Reject all other pending offers for this suborder (ATOMIC)
            List<RiderOffer> otherOffers = riderOfferRepository.findPendingOffersForSubOrder(
                    lockedSubOrder, LocalDateTime.now());
            for (RiderOffer otherOffer : otherOffers) {
                if (!otherOffer.getId().equals(offerId)) {
                    otherOffer.setStatus("REJECTED");
                    riderOfferRepository.save(otherOffer);
                    
                    // Notify other riders that order was taken
                    if (otherOffer.getRider() != null && otherOffer.getRider().getId() != riderId) {
                        Map<String, Object> notificationPayload = new HashMap<>();
                        notificationPayload.put("subOrderId", lockedSubOrder.getId());
                        notificationPayload.put("multiOrderId", lockedSubOrder.getMultiOrder() != null ? lockedSubOrder.getMultiOrder().getId() : null);
                        notificationPayload.put("status", "TAKEN");
                        notificationPayload.put("message", "This order was accepted by another rider");
                        webSocketPublisher.publishToRider(otherOffer.getRider().getId(), "ORDER_TAKEN", notificationPayload);
                    }
                }
            }
            
            // Assign rider to suborder (ATOMIC)
            lockedSubOrder.setRider(rider);
            lockedSubOrder.setStatus("ACCEPTED"); // Start tracking after acceptance
            subOrderRepository.save(lockedSubOrder);
            
            rider.setStatus("ACCEPTED");
            riderRepository.save(rider);
            
            logger.info("Rider {} accepted offer {} for SubOrder {} (ATOMIC)", riderId, offerId, lockedSubOrder.getId());
            
            // Send WebSocket notification to customer and rider
            if (lockedSubOrder.getMultiOrder() != null) {
                webSocketPublisher.publishOrderUpdate(
                        lockedSubOrder.getMultiOrder().getId(),
                        lockedSubOrder.getId(),
                        "ACCEPTED",
                        rider.getId(),
                        rider.getCurrentLatitude() != null ? rider.getCurrentLatitude().doubleValue() : null,
                        rider.getCurrentLongitude() != null ? rider.getCurrentLongitude().doubleValue() : null);
                
                // Notify rider that order is now assigned and tracking started
                Map<String, Object> notificationPayload = new HashMap<>();
                notificationPayload.put("subOrderId", lockedSubOrder.getId());
                notificationPayload.put("multiOrderId", lockedSubOrder.getMultiOrder().getId());
                notificationPayload.put("status", "ACCEPTED");
                notificationPayload.put("message", "Order accepted! Tracking has started.");
                webSocketPublisher.publishToRider(rider.getId(), "ORDER_ACCEPTED", notificationPayload);
            }
            
            return true;
        } else {
            // Reject the offer
            offer.setStatus("REJECTED");
            riderOfferRepository.save(offer);
            
            logger.info("Rider {} rejected offer {} for SubOrder {}", riderId, offerId, subOrder.getId());
            
            // Try to find another rider and send offer
            int attempts = getAssignmentAttempts(subOrder);
            if (attempts < MAX_ASSIGNMENT_ATTEMPTS) {
                incrementAssignmentAttempts(subOrder);
                
                // Get all previously rejected rider IDs (including the current one)
                List<RiderOffer> previousRejections = riderOfferRepository.findBySubOrderAndStatus(
                        subOrder, "REJECTED");
                List<Long> rejectedRiderIds = new ArrayList<>();
                for (RiderOffer off : previousRejections) {
                    rejectedRiderIds.add(off.getRider().getId());
                }
                
                // Add current rejecting rider if not already in the list
                if (!rejectedRiderIds.contains(riderId)) {
                    rejectedRiderIds.add(riderId);
                }
                
                // Try to send offer to next nearest rider, excluding all rejected riders
                Optional<RiderOffer> newOffer = sendOfferToNearestRider(
                        subOrder.getId(), 
                        AssignmentStrategy.NEAREST, 
                        rejectedRiderIds);
                
                        if (newOffer.isPresent()) {
                    logger.info("Sent new offer to rider {} for SubOrder {} after rejection by rider {}", 
                            newOffer.get().getRider().getId(), subOrder.getId(), riderId);
                            return true;
                } else {
                    logger.warn("No more available riders for SubOrder {} after rejection by rider {}", 
                            subOrder.getId(), riderId);
                }
            }
            
            // No more riders available - mark order as pending
            subOrder.setStatus("PENDING");
            subOrderRepository.save(subOrder);
            
            logger.warn("SubOrder {} could not be assigned after {} attempts", 
                    subOrder.getId(), attempts);
            return false;
        }
    }

    /**
     * Legacy method for backward compatibility - finds offer by subOrderId
     * This method is kept for backward compatibility with existing code that calls
     * handleRiderOfferResponse with subOrderId instead of offerId
     */
    public boolean handleRiderOfferResponse(Long riderId, Long subOrderId, boolean accept) {
        // Find the offer for this rider and suborder
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));
        
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));
        
        Optional<RiderOffer> offerOpt = riderOfferRepository.findBySubOrderAndRiderAndStatus(
                subOrder, rider, "PENDING");
        
        if (offerOpt.isPresent()) {
            // Use the new method with offerId
            return handleRiderOfferResponseByOfferId(riderId, offerOpt.get().getId(), accept);
        } else {
            // Fallback to old behavior if no offer exists (for backward compatibility)
            if (accept && subOrder.getRider() != null && subOrder.getRider().getId().equals(riderId)) {
                subOrder.setStatus("ACCEPTED");
                subOrderRepository.save(subOrder);
                return true;
            }
            return false;
        }
    }

    /**
     * Updates rider location
     */
    public void updateRiderLocation(Long riderId, BigDecimal latitude, BigDecimal longitude) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        rider.setCurrentLatitude(latitude);
        rider.setCurrentLongitude(longitude);
        riderRepository.save(rider);
        
        logger.debug("Updated location for rider {}: lat={}, lon={}", 
                riderId, latitude, longitude);
    }

    /**
     * Updates rider online status
     */
    public void toggleOnlineStatus(Long riderId, boolean isOnline) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        rider.setIsOnline(isOnline);
        if (!isOnline) {
            rider.setIsAvailable(false);
            rider.setStatus("OFFLINE");
        } else {
            rider.setIsAvailable(true);
            rider.setStatus("IDLE");
        }
        riderRepository.save(rider);
        
        logger.info("Rider {} is now {}", riderId, isOnline ? "ONLINE" : "OFFLINE");
    }

    /**
     * Updates rider status
     */
    public void updateRiderStatus(Long riderId, String status) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        rider.setStatus(status);
        riderRepository.save(rider);
        
        logger.info("Updated status for rider {}: {}", riderId, status);
    }

    /**
     * Gets or creates a rider profile for a user
     */
    public Rider getOrCreateRider(User user) {
        return riderRepository.findByUser(user)
                .orElseGet(() -> {
                    Rider rider = new Rider();
                    rider.setUser(user);
                    rider.setPhoneNumber(user.getPhoneNumber());
                    rider.setIsOnline(false);
                    rider.setIsAvailable(true);
                    rider.setStatus("IDLE");
                    return riderRepository.save(rider);
                });
    }

    private List<Rider> getAvailableRidersForRestaurant(Restaurant restaurant) {
        // Get riders associated with the restaurant
        List<Rider> restaurantRiders = restaurantRiderRepository
                .findByRestaurantAndIsActiveTrue(restaurant)
                .stream()
                .map(rr -> rr.getRider())
                .filter(rider -> rider.getIsOnline() && rider.getIsAvailable())
                .toList();

        // If no restaurant-specific riders, fall back to all online available riders
        if (restaurantRiders.isEmpty()) {
            return riderRepository.findOnlineAvailableRidersWithLocation();
        }

        return restaurantRiders;
    }

    private Rider selectNearestRider(List<Rider> candidates, Restaurant restaurant) {
        if (candidates.isEmpty()) {
            return null;
        }

        // Filter riders with valid location
        List<Rider> ridersWithLocation = candidates.stream()
                .filter(r -> r.getCurrentLatitude() != null && r.getCurrentLongitude() != null)
                .toList();
        
        if (ridersWithLocation.isEmpty()) {
            // No riders with location - return first available
            logger.warn("No riders with location data, using first available rider");
            return candidates.get(0);
        }
        
        // If restaurant has coordinates, use them to find nearest rider
        if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
            // Use repository's nearest rider query (Haversine formula)
            List<Rider> nearestRiders = riderRepository.findNearestRiders(
                    restaurant.getLatitude(),
                    restaurant.getLongitude(),
                    10); // Get top 10 nearest
            
            // Find the first rider from nearest list that's in our candidates
            for (Rider nearest : nearestRiders) {
                if (ridersWithLocation.contains(nearest)) {
                    logger.info("Selected nearest rider {} for restaurant {} (distance calculated)", 
                            nearest.getId(), restaurant.getId());
                    return nearest;
                }
            }
            
            // If none of the nearest riders are in candidates, use least loaded from candidates
            logger.warn("Nearest riders not in candidate list, using least loaded from candidates");
            return selectLeastLoadedRider(ridersWithLocation);
        }
        
        // Restaurant doesn't have coordinates - use least loaded rider among those with location
        logger.info("Restaurant {} has no coordinates, using least loaded rider", restaurant.getId());
        return selectLeastLoadedRider(ridersWithLocation);
    }

    private Rider selectLeastLoadedRider(List<Rider> candidates) {
        // Count active assignments per rider
        return candidates.stream()
                .min((r1, r2) -> {
                    long count1 = subOrderRepository.findByRiderAndStatusInOrderByCreatedAtDesc(
                            r1, List.of("ASSIGNED", "ACCEPTED", "EN_ROUTE")).size();
                    long count2 = subOrderRepository.findByRiderAndStatusInOrderByCreatedAtDesc(
                            r2, List.of("ASSIGNED", "ACCEPTED", "EN_ROUTE")).size();
                    return Long.compare(count1, count2);
                })
                .orElse(candidates.get(0));
    }

    private int getAssignmentAttempts(SubOrder subOrder) {
        // Store attempts in tracking info or use a separate field
        // For now, return 0 (would need to track this properly)
        return 0;
    }

    private void incrementAssignmentAttempts(SubOrder subOrder) {
        // Increment attempt counter
        // For now, just log
        logger.debug("Incremented assignment attempts for SubOrder {}", subOrder.getId());
    }

    /**
     * Calculates distance between two coordinates using Haversine formula
     */
    private double calculateDistance(BigDecimal lat1, BigDecimal lon1, 
                                     BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        final int R = 6371; // Earth radius in km

        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}

