package com.srFoodDelivery.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Offer;
import com.srFoodDelivery.model.Restaurant;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

       List<Offer> findByRestaurantAndIsActiveTrueOrderByCreatedAtDesc(Restaurant restaurant);

       @Query("SELECT o FROM Offer o WHERE o.isActive = true " +
                     "AND o.startDate <= :now AND o.endDate >= :now " +
                     "ORDER BY o.createdAt DESC")
       List<Offer> findActiveOffers(@Param("now") LocalDateTime now);

       @Query("SELECT o FROM Offer o WHERE o.isActive = true " +
                     "AND o.startDate <= :now AND o.endDate >= :now " +
                     "AND o.restaurant.isCafeLounge = :isCafe " +
                     "ORDER BY o.createdAt DESC")
       List<Offer> findActiveOffersByCafeFlag(@Param("now") LocalDateTime now,
                     @Param("isCafe") boolean isCafe);

       @Query("SELECT o FROM Offer o WHERE o.restaurant = :restaurant " +
                     "AND o.isActive = true " +
                     "AND o.startDate <= :now AND o.endDate >= :now " +
                     "ORDER BY o.createdAt DESC")
       List<Offer> findActiveOffersByRestaurant(@Param("restaurant") Restaurant restaurant,
                     @Param("now") LocalDateTime now);

       List<Offer> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);

       @Query("SELECT o FROM Offer o WHERE o.restaurant.id = :restaurantId " +
                     "AND o.couponCode = :code " +
                     "AND o.isActive = true " +
                     "AND o.startDate <= :now AND o.endDate >= :now")
       Offer findActiveCouponByCode(@Param("code") String code,
                     @Param("restaurantId") Long restaurantId,
                     @Param("now") LocalDateTime now);
}
