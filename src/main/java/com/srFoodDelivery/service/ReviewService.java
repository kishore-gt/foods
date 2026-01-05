package com.srFoodDelivery.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.ReviewForm;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.Review;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.ReviewRepository;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public ReviewService(ReviewRepository reviewRepository,
                        RestaurantRepository restaurantRepository,
                        MenuItemRepository menuItemRepository) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public Review createReview(User user, ReviewForm form) {
        Review review = new Review();
        review.setUser(user);
        review.setRating(form.getRating());
        review.setComment(form.getComment());

        if (form.getRestaurantId() != null) {
            Restaurant restaurant = restaurantRepository.findById(form.getRestaurantId())
                    .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            if (reviewRepository.existsByUserAndRestaurant(user, restaurant)) {
                throw new IllegalArgumentException("You have already reviewed this restaurant");
            }
            review.setRestaurant(restaurant);
        } else if (form.getMenuItemId() != null) {
            MenuItem menuItem = menuItemRepository.findById(form.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
            if (reviewRepository.existsByUserAndMenuItem(user, menuItem)) {
                throw new IllegalArgumentException("You have already reviewed this menu item");
            }
            review.setMenuItem(menuItem);
        } else {
            throw new IllegalArgumentException("Either restaurant or menu item must be specified");
        }

        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> getRestaurantReviews(Restaurant restaurant) {
        return reviewRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    @Transactional(readOnly = true)
    public List<Review> getMenuItemReviews(MenuItem menuItem) {
        return reviewRepository.findByMenuItemOrderByCreatedAtDesc(menuItem);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Restaurant restaurant) {
        List<Review> reviews = reviewRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(MenuItem menuItem) {
        List<Review> reviews = reviewRepository.findByMenuItemOrderByCreatedAtDesc(menuItem);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}

