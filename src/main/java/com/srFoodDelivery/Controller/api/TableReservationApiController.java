package com.srFoodDelivery.Controller.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.TableReservation;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.TableService;
import com.srFoodDelivery.service.RestaurantService;

@RestController
@RequestMapping("/api/table-reservations")
public class TableReservationApiController {

    private final TableService tableService;
    private final RestaurantService restaurantService;

    public TableReservationApiController(TableService tableService, RestaurantService restaurantService) {
        this.tableService = tableService;
        this.restaurantService = restaurantService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTableStatuses(
            @RequestParam Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "60") Integer durationMinutes,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // Get all active tables to ensure we cover every table
        List<RestaurantTable> allTables = tableService.getActiveTablesByRestaurant(restaurantId);

        // Check availability logic
        // We want to know for each table: AVAILABLE, HELD_BY_ME, HELD_BY_OTHERS, BOOKED

        // This is slightly inefficient if we iterate all tables and call
        // isTableAvailable for each.
        // A better approach would be to get all conflicting reservations in one query,
        // but TableService doesn't expose that directly yet.
        // For now, we'll iterate since table count per restaurant is low (< 100).

        List<Map<String, Object>> statuses = allTables.stream().map(table -> {
            Map<String, Object> status = new HashMap<>();
            status.put("tableId", table.getId());

            // Check for specific reservations that conflict
            // We need a way to find *who* holds the reservation to distinguish HELD_BY_ME

            // Let's use the valid conflicting reservations
            // Implementation detail: TableService.isTableAvailable returns boolean.
            // We need to fetch the actual conflicting reservations.
            // Since we can't easily change Service without affecting others, let's use what
            // we have or add a method.
            // Actually, we can just use `tableReservationRepository` logic if we had
            // access, but proper layering requires Service.
            // Let's rely on `isTableAvailable` for "AVAILABLE" status.

            // However, distinguishing "HELD_BY_ME" vs "HELD_BY_OTHERS" is crucial.
            // Let's add a method to TableService or query here?
            // Better to stick to Service.

            // Workaround: We will fetch ALL reservations for that restaurant/date and
            // filter in memory.
            return status;
        }).collect(Collectors.toList());

        // Optimized approach:
        List<TableReservation> manualReservations = tableService.getReservationsByRestaurantAndDate(restaurantId, date);

        LocalTime endTime = time.plusMinutes(durationMinutes);
        Long currentUserId = (principal != null) ? principal.getUser().getId() : -1L;

        Map<Long, String> tableStatusMap = new HashMap<>();

        for (RestaurantTable table : allTables) {
            // Default to available
            String currentStatus = "AVAILABLE";

            // Find conflicts
            for (TableReservation res : manualReservations) {
                if (res.getTable().getId().equals(table.getId()) &&
                        !res.getStatus().equals("CANCELLED") &&
                        !res.getStatus().equals("COMPLETED")) {

                    // Check time overlap
                    LocalTime resStart = res.getReservationTime();
                    LocalTime resEnd = resStart.plusMinutes(res.getDurationMinutes());

                    if (resStart.isBefore(endTime) && resEnd.isAfter(time)) {
                        // Overlap found
                        if ("PENDING".equals(res.getStatus())) {
                            if (res.getUser().getId().equals(currentUserId)) {
                                currentStatus = "HELD_BY_ME";
                            } else {
                                currentStatus = "HELD_BY_OTHERS"; // or OCCUPIED
                            }
                        } else {
                            currentStatus = "BOOKED"; // CONFIRMED or CHECKED_IN
                        }
                        break; // Found a conflict, no need to check other reservations for this table
                    }
                }
            }
            tableStatusMap.put(table.getId(), currentStatus);
        }

        return ResponseEntity.ok(Map.of("statuses", tableStatusMap));
    }

    @PostMapping("/hold")
    public ResponseEntity<?> holdTable(
            @RequestParam Long restaurantId,
            @RequestParam Long tableId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "60") Integer durationMinutes,
            @RequestParam(defaultValue = "2") Integer numberOfGuests,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in");
        }

        try {
            TableReservation reservation = tableService.createReservation(
                    principal.getUser(), restaurantId, tableId, date, time, durationMinutes, numberOfGuests,
                    "Hold for booking");

            Map<String, Object> response = new HashMap<>();
            response.put("reservationId", reservation.getId());
            response.put("status", reservation.getStatus());
            response.put("message", "Table held successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error holding table: " + e.getMessage());
        }
    }

    @DeleteMapping("/hold/{reservationId}")
    public ResponseEntity<?> releaseHold(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in");
        }

        try {
            // We need a method to cancel reservation. TableService.checkOut completes it,
            // but we want cancel.
            // Using a repository or service method directly?
            // TableService doesn't have a generic "cancel" method visible in the outline.
            // It has `autoReleaseExpiredReservations`.
            // I should add `cancelReservation` to TableService for cleanliness.
            // But since I can't modify TableService just yet (waiting for next step), I'll
            // note to add it.
            // For now, I'll assume I'll add `cancelReservation` to TableService.

            // Wait, I am writing this file, I can modifying TableService in the next step.
            // I'll call `tableService.cancelReservation(reservationId,
            // principal.getUser())`.
            tableService.cancelReservation(reservationId, principal.getUser());

            return ResponseEntity.ok(Map.of("message", "Hold released"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
