package com.srFoodDelivery.Controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.model.RatingReview;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.RatingReviewRepository;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.security.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/restaurants")
public class RatingApiController {

    private final RatingReviewRepository ratingReviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final SubOrderRepository subOrderRepository;

    public RatingApiController(
            RatingReviewRepository ratingReviewRepository,
            RestaurantRepository restaurantRepository,
            SubOrderRepository subOrderRepository) {
        this.ratingReviewRepository = ratingReviewRepository;
        this.restaurantRepository = restaurantRepository;
        this.subOrderRepository = subOrderRepository;
    }

    @PostMapping("/{restaurantId}/reviews")
    public ResponseEntity<Map<String, Object>> createReview(
            @PathVariable Long restaurantId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        User user = principal.getUser();
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        SubOrder subOrder = subOrderRepository.findById(request.getSubOrderId())
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));

        // Verify suborder belongs to user
        if (!subOrder.getMultiOrder().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied");
        }

        // Verify suborder is delivered/completed
        if (!"DELIVERED".equals(subOrder.getStatus()) && !"COMPLETED".equals(subOrder.getStatus())) {
            throw new IllegalStateException("Can only review delivered or completed orders");
        }

        // Check if already reviewed
        if (ratingReviewRepository.findByUserAndSubOrder(user, subOrder).isPresent()) {
            throw new IllegalStateException("Order already reviewed");
        }

        // Create rating review
        RatingReview review = new RatingReview();
        review.setUser(user);
        review.setRestaurant(restaurant);
        review.setSubOrder(subOrder);
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        review.setIsVerified(true); // Auto-verify for delivered orders

        RatingReview saved = ratingReviewRepository.save(review);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reviewId", saved.getId());
        response.put("message", "Review submitted successfully");
        return ResponseEntity.ok(response);
    }

    public static class ReviewRequest {
        @NotNull(message = "SubOrder ID is required")
        private Long subOrderId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        private Integer rating;

        private String reviewText;

        // Getters and Setters
        public Long getSubOrderId() {
            return subOrderId;
        }

        public void setSubOrderId(Long subOrderId) {
            this.subOrderId = subOrderId;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public String getReviewText() {
            return reviewText;
        }

        public void setReviewText(String reviewText) {
            this.reviewText = reviewText;
        }
    }
}

