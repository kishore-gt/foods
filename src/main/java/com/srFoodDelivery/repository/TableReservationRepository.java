package com.srFoodDelivery.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.TableReservation;
import com.srFoodDelivery.model.User;

@Repository
public interface TableReservationRepository extends JpaRepository<TableReservation, Long> {
    
    List<TableReservation> findByRestaurantAndReservationDate(Restaurant restaurant, LocalDate date);
    
    List<TableReservation> findByTableAndReservationDate(RestaurantTable table, LocalDate date);
    
    List<TableReservation> findByUserOrderByReservationDateDescReservationTimeDesc(User user);
    
    Optional<TableReservation> findByQrCode(String qrCode);
    
    @Query("SELECT r FROM TableReservation r WHERE r.table = :table " +
           "AND r.reservationDate = :date " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN') " +
           "AND ((r.reservationTime <= :endTime AND FUNCTION('ADDTIME', r.reservationTime, FUNCTION('SEC_TO_TIME', r.durationMinutes * 60)) >= :startTime) OR " +
           "(r.reservationTime >= :startTime AND r.reservationTime < :endTime))")
    List<TableReservation> findConflictingReservations(
        @Param("table") RestaurantTable table,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    @Query("SELECT r FROM TableReservation r WHERE r.restaurant = :restaurant " +
           "AND r.reservationDate = :date " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')")
    List<TableReservation> findActiveReservationsByRestaurantAndDate(
        @Param("restaurant") Restaurant restaurant,
        @Param("date") LocalDate date
    );
    
    @Query("SELECT r FROM TableReservation r WHERE r.autoReleaseTime <= :now " +
           "AND r.status = 'PENDING'")
    List<TableReservation> findReservationsToAutoRelease(@Param("now") LocalDateTime now);
}

