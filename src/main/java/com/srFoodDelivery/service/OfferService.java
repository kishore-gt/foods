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
            // Simple matching - in production, you'd have a separate coupon code field
            if (offer.getTitle().toUpperCase().contains(couponCode.toUpperCase()) ||
                (offer.getDescription() != null && offer.getDescription().toUpperCase().contains(couponCode.toUpperCase()))) {
                
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
                
                // Calculate discount based on offer type
                BigDecimal discount = calculateDiscount(offer, cartTotal);
                return discount;
            }
        }
        
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateDiscount(Offer offer, BigDecimal cartTotal) {
        if (offer.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        
        switch (offer.getOfferType()) {
            case "PERCENTAGE_OFF":
                discount = cartTotal.multiply(offer.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;
            case "FLAT_DISCOUNT":
                discount = offer.getDiscountValue();
                break;
            case "BUY_ONE_GET_ONE":
                // For BOGO, calculate based on items (simplified)
                discount = cartTotal.multiply(BigDecimal.valueOf(0.5))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                break;
            default:
                return BigDecimal.ZERO;
        }
        
        // Apply maximum discount cap if set
        if (offer.getMaxDiscount() != null && discount.compareTo(offer.getMaxDiscount()) > 0) {
            discount = offer.getMaxDiscount();
        }
        
        // Ensure discount doesn't exceed cart total
        if (discount.compareTo(cartTotal) > 0) {
            discount = cartTotal;
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
}
