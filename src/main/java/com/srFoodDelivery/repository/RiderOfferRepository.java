package com.srFoodDelivery.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Rider;
import com.srFoodDelivery.model.RiderOffer;
import com.srFoodDelivery.model.SubOrder;

@Repository
public interface RiderOfferRepository extends JpaRepository<RiderOffer, Long> {
    
    List<RiderOffer> findBySubOrderOrderByCreatedAtDesc(SubOrder subOrder);
    
    List<RiderOffer> findByRiderOrderByCreatedAtDesc(Rider rider);
    
    Optional<RiderOffer> findBySubOrderAndRiderAndStatus(SubOrder subOrder, Rider rider, String status);
    
    List<RiderOffer> findBySubOrderAndStatus(SubOrder subOrder, String status);
    
    @Query("SELECT ro FROM RiderOffer ro WHERE ro.subOrder = :subOrder AND ro.status = 'PENDING' AND ro.expiresAt > :now ORDER BY ro.createdAt DESC")
    List<RiderOffer> findPendingOffersForSubOrder(@Param("subOrder") SubOrder subOrder, @Param("now") LocalDateTime now);
    
    @Query("SELECT ro FROM RiderOffer ro WHERE ro.rider = :rider AND ro.status = 'PENDING' AND ro.expiresAt > :now ORDER BY ro.createdAt DESC")
    List<RiderOffer> findPendingOffersForRider(@Param("rider") Rider rider, @Param("now") LocalDateTime now);
    
    @Query("SELECT ro FROM RiderOffer ro WHERE ro.status = 'PENDING' AND ro.expiresAt < :now")
    List<RiderOffer> findExpiredOffers(@Param("now") LocalDateTime now);
}

