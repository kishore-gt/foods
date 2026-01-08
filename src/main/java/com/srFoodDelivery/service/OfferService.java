package com.srFoodDelivery.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.Cart;
import com.srFoodDelivery.model.Offer;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SiteMode;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.OfferRepository;

@Service
@Transactional(readOnly = true)
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    public List<Offer> getActiveOffers() {
        return getActiveOffersForMode(null);
    }

    public List<Offer> getActiveOffersForMode(SiteMode siteMode) {
        LocalDateTime now = LocalDateTime.now();
        if (siteMode != null && siteMode.isCafeMode()) {
            return offerRepository.findActiveOffersByCafeFlag(now, true);
        }
        return offerRepository.findActiveOffers(now);
    }

    public List<Offer> getActiveOffersByRestaurant(Restaurant restaurant) {
        return offerRepository.findActiveOffersByRestaurant(restaurant, LocalDateTime.now());
    }

    public List<Offer> getAllOffersByRestaurant(Restaurant restaurant) {
        return offerRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    /**
     * Get available offers for a user's cart
     */
    public List<Offer> getAvailableOffers(User user, Cart cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            return new ArrayList<>();
        }

        BigDecimal cartTotal = cart.getTotalAmount();
        LocalDateTime now = LocalDateTime.now();

        // Get all active offers
        List<Offer> activeOffers = offerRepository.findActiveOffers(now);

        // Filter offers that are applicable to this cart
        return activeOffers.stream()
                .filter(offer -> {
                    // Check if cart total meets minimum order amount
                    if (offer.getMinOrderAmount() != null &&
                            cartTotal.compareTo(offer.getMinOrderAmount()) < 0) {
                        return false;
                    }

                    // Check if offer is for a specific restaurant in the cart
                    if (cart.getRestaurant() != null) {
                        return offer.getRestaurant().getId().equals(cart.getRestaurant().getId());
                    }

                    // For multi-restaurant carts, show all applicable offers
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Apply discount based on coupon code
     */
    @Transactional
    public BigDecimal applyDiscount(String couponCode, User user, Cart cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal cartTotal = cart.getTotalAmount();
        LocalDateTime now = LocalDateTime.now();

        // Find offer by matching coupon code (using title or description)
        List<Offer> activeOffers = offerRepository.findActiveOffers(now);

        for (Offer offer : activeOffers) {
            // detailed matching logic
            boolean matchesCode = (offer.getCouponCode() != null && offer.getCouponCode().equalsIgnoreCase(couponCode));
            boolean matchesTitle = (offer.getCouponCode() == null || offer.getCouponCode().isEmpty()) &&
                    (offer.getTitle().toUpperCase().contains(couponCode.toUpperCase()) ||
                            (offer.getDescription() != null
                                    && offer.getDescription().toUpperCase().contains(couponCode.toUpperCase())));

            if (matchesCode || matchesTitle) {

                // Check minimum order amount
                if (offer.getMinOrderAmount() != null &&
                        cartTotal.compareTo(offer.getMinOrderAmount()) < 0) {
                    continue;
                }

                // Check if offer is for the cart's restaurant
                if (cart.getRestaurant() != null &&
                        !offer.getRestaurant().getId().equals(cart.getRestaurant().getId())) {
                    continue;
                }

                // Check if offer is applicable to items in the cart
                // If applicableMenuItemIds is set, at least one item must match
                List<Long> applicableIds = offer.getApplicableMenuItemIdList();
                if (!applicableIds.isEmpty()) {
                    boolean hasApplicableItem = cart.getItems().stream()
                            .anyMatch(item -> applicableIds.contains(item.getMenuItem().getId()));
                    if (!hasApplicableItem) {
                        continue;
                    }
                }

                // Calculate discount based on offer type
                BigDecimal discount = calculateDiscount(offer, cart);
                return discount;
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calculateDiscount(Offer offer, Cart cart) {
        if (offer.getDiscountValue() == null && !"FREE_DELIVERY".equals(offer.getOfferType())) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        List<Long> applicableIds = offer.getApplicableMenuItemIdList();

        // Calculate relevant total (either full cart or specific items)
        BigDecimal relevantTotal = BigDecimal.ZERO;

        if (applicableIds.isEmpty()) {
            relevantTotal = cart.getTotalAmount();
        } else {
            relevantTotal = cart.getItems().stream()
                    .filter(item -> applicableIds.contains(item.getMenuItem().getId()))
                    .map(item -> item.getLineTotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // If relevant total is zero (e.g. items not in cart), return 0
        if (relevantTotal.compareTo(BigDecimal.ZERO) == 0 && !"FREE_DELIVERY".equals(offer.getOfferType())) {
            return BigDecimal.ZERO;
        }

        switch (offer.getOfferType()) {
            case "PERCENTAGE_OFF":
                discount = relevantTotal.multiply(offer.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            case "FLAT_DISCOUNT":
                // Flat discount applies once to the order
                discount = offer.getDiscountValue();
                break;

            case "BUY_ONE_GET_ONE":
                // Logic: For every 2 of the SAME item, 1 is free.
                // If applicableIds are present, only apply to those items.
                // If empty, apply to ALL items.

                for (var item : cart.getItems()) {
                    // Skip if item is not applicable (when specific IDs are defined)
                    if (!applicableIds.isEmpty() && !applicableIds.contains(item.getMenuItem().getId())) {
                        continue;
                    }

                    int quantity = item.getQuantity();
                    if (quantity >= 2) {
                        int freeItems = quantity / 2;
                        BigDecimal itemDiscount = item.getUnitPrice().multiply(BigDecimal.valueOf(freeItems));
                        discount = discount.add(itemDiscount);
                    }
                }
                break;

            case "FREE_DELIVERY":
                // Delivery logic is currently not explicit in cart total.
                // Placeholder: return 0 or specific fee if/when added.
                // Since we don't have a fee in cart, we can't discount it yet.
                discount = BigDecimal.ZERO;
                break;

            case "COMBO_OFFER":
                // Placeholder for complex combo logic
                discount = offer.getDiscountValue();
                break;

            default:
                return BigDecimal.ZERO;
        }

        // Apply maximum discount cap if set
        if (offer.getMaxDiscount() != null && discount.compareTo(offer.getMaxDiscount()) > 0) {
            discount = offer.getMaxDiscount();
        }

        // Ensure discount doesn't exceed cart total
        if (discount.compareTo(cart.getTotalAmount()) > 0) {
            discount = cart.getTotalAmount();
        }

        return discount;
    }

    @Transactional
    public Offer createOffer(Offer offer) {
        return offerRepository.save(offer);
    }

    @Transactional
    public Offer updateOffer(Long id, Offer offerDetails) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        offer.setTitle(offerDetails.getTitle());
        offer.setDescription(offerDetails.getDescription());
        offer.setCouponCode(offerDetails.getCouponCode());
        offer.setOfferType(offerDetails.getOfferType());
        offer.setDiscountValue(offerDetails.getDiscountValue());
        offer.setMinOrderAmount(offerDetails.getMinOrderAmount());
        offer.setMaxDiscount(offerDetails.getMaxDiscount());
        offer.setStartDate(offerDetails.getStartDate());
        offer.setEndDate(offerDetails.getEndDate());
        offer.setActive(offerDetails.isActive());
        offer.setImageUrl(offerDetails.getImageUrl());

        return offerRepository.save(offer);
    }

    @Transactional
    public void deleteOffer(Long id) {
        offerRepository.deleteById(id);
    }

    public Offer getOfferById(Long id) {
        return offerRepository.findById(id).orElse(null);
    }
}
