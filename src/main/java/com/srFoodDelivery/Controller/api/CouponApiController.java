package com.srFoodDelivery.Controller.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.model.Offer;
import com.srFoodDelivery.repository.OfferRepository;

@RestController
@RequestMapping("/api/coupons")
public class CouponApiController {

    private final OfferRepository offerRepository;

    public CouponApiController(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @RequestParam String code,
            @RequestParam Long restaurantId,
            @RequestParam BigDecimal orderAmount) {

        Offer offer = offerRepository.findActiveCouponByCode(code, restaurantId, LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();

        if (offer == null) {
            response.put("valid", false);
            response.put("message", "Invalid or expired coupon code");
            return ResponseEntity.ok(response);
        }

        if (orderAmount.compareTo(offer.getMinOrderAmount()) < 0) {
            response.put("valid", false);
            response.put("message", "Minimum order amount is ₹" + offer.getMinOrderAmount());
            return ResponseEntity.ok(response);
        }

        // Calculate discount
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENTAGE_OFF".equals(offer.getOfferType())) {
            discount = orderAmount.multiply(offer.getDiscountValue().divide(new BigDecimal(100)));
            if (offer.getMaxDiscount() != null && discount.compareTo(offer.getMaxDiscount()) > 0) {
                discount = offer.getMaxDiscount();
            }
        } else if ("FLAT_DISCOUNT".equals(offer.getOfferType())) {
            discount = offer.getDiscountValue();
        }

        response.put("valid", true);
        response.put("offerId", offer.getId());
        response.put("code", offer.getCouponCode());
        response.put("discountAmount", discount);
        response.put("message", "Coupon applied: " + offer.getTitle());

        return ResponseEntity.ok(response);
    }
}
