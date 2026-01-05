package com.srFoodDelivery.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Rider;
import com.srFoodDelivery.model.User;

@Repository
public interface RiderRepository extends JpaRepository<Rider, Long> {
    
    Optional<Rider> findByUser(User user);
    
    List<Rider> findByIsOnlineTrueAndIsAvailableTrue();
    
    @Query("SELECT r FROM Rider r WHERE r.isOnline = true AND r.isAvailable = true " +
           "AND r.currentLatitude IS NOT NULL AND r.currentLongitude IS NOT NULL")
    List<Rider> findOnlineAvailableRidersWithLocation();
    
    @Query(value = "SELECT r.* FROM rider r " +
           "WHERE r.is_online = true " +
           "AND r.is_available = true " +
           "AND r.current_latitude IS NOT NULL " +
           "AND r.current_longitude IS NOT NULL " +
           "ORDER BY (6371 * acos(cos(radians(?1)) * cos(radians(r.current_latitude)) * " +
           "cos(radians(r.current_longitude) - radians(?2)) + " +
           "sin(radians(?1)) * sin(radians(r.current_latitude)))) ASC " 
           , nativeQuery = true)
    List<Rider> findNearestRiders(
        BigDecimal latitude,
        BigDecimal longitude,
        int limit
    );
    
    @Query("SELECT r FROM Rider r WHERE r.status IN :statuses")
    List<Rider> findByStatusIn(@Param("statuses") List<String> statuses);
}

