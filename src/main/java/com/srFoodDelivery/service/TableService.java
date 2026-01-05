package com.srFoodDelivery.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.TableReservation;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.RestaurantTableRepository;
import com.srFoodDelivery.repository.TableReservationRepository;

@Service
@Transactional
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final TableReservationRepository reservationRepository;
    private final RestaurantService restaurantService;

    public TableService(RestaurantTableRepository tableRepository,
            TableReservationRepository reservationRepository,
            RestaurantService restaurantService) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.restaurantService = restaurantService;
    }

    // Table Management
    @Transactional(readOnly = true)
    public List<RestaurantTable> getActiveTablesByRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantService.getById(restaurantId);
        return tableRepository.findActiveTablesByRestaurantOrdered(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getTablesByRestaurantAndType(Long restaurantId, String tableType) {
        Restaurant restaurant = restaurantService.getById(restaurantId);
        return tableRepository.findByRestaurantAndTableTypeAndIsActiveTrue(restaurant, tableType);
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getTablesByRestaurantAndSection(Long restaurantId, String sectionName) {
        Restaurant restaurant = restaurantService.getById(restaurantId);
        return tableRepository.findByRestaurantAndSectionNameAndIsActiveTrue(restaurant, sectionName);
    }

    @Transactional(readOnly = true)
    public Optional<RestaurantTable> getTableById(Long tableId) {
        return tableRepository.findById(tableId);
    }

    // Reservation Management
    @Transactional(readOnly = true)
    public List<TableReservation> getReservationsByRestaurantAndDate(Long restaurantId, LocalDate date) {
        Restaurant restaurant = restaurantService.getById(restaurantId);
        return reservationRepository.findActiveReservationsByRestaurantAndDate(restaurant, date);
    }

    @Transactional(readOnly = true)
    public List<TableReservation> getUserReservations(User user) {
        return reservationRepository.findByUserOrderByReservationDateDescReservationTimeDesc(user);
    }

    @Transactional(readOnly = true)
    public boolean isTableAvailable(Long tableId, LocalDate date, LocalTime startTime, Integer durationMinutes) {
        Optional<RestaurantTable> tableOpt = tableRepository.findById(tableId);
        if (tableOpt.isEmpty()) {
            return false;
        }

        RestaurantTable table = tableOpt.get();
        LocalTime endTime = startTime.plusMinutes(durationMinutes);

        List<TableReservation> conflicts = reservationRepository.findConflictingReservations(
                table, date, startTime, endTime);

        return conflicts.isEmpty();
    }

    public TableReservation createReservation(User user, Long restaurantId, Long tableId,
            LocalDate date, LocalTime time, Integer durationMinutes,
            Integer numberOfGuests, String specialRequests) {
        Restaurant restaurant = restaurantService.getById(restaurantId);

        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        if (!table.getIsActive()) {
            throw new IllegalStateException("Table is not active");
        }

        // Check availability
        if (!isTableAvailable(tableId, date, time, durationMinutes)) {
            throw new IllegalStateException(
                    "This table is already booked at that time. Please select another table or time slot.");
        }

        TableReservation reservation = new TableReservation();
        reservation.setRestaurant(restaurant);
        reservation.setTable(table);
        reservation.setUser(user);
        reservation.setReservationDate(date);
        reservation.setReservationTime(time);
        reservation.setDurationMinutes(durationMinutes);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setSpecialRequests(specialRequests);
        reservation.setStatus("PENDING");

        // Generate QR code
        String qrCode = generateQRCode(restaurantId, tableId);
        reservation.setQrCode(qrCode);

        // Set auto-release time (15 minutes after reservation time if not checked in)
        LocalDateTime reservationDateTime = LocalDateTime.of(date, time);
        reservation.setAutoReleaseTime(reservationDateTime.plusMinutes(15));

        return reservationRepository.save(reservation);
    }

    public TableReservation checkIn(String qrCode) {
        TableReservation reservation = reservationRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR code"));

        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            throw new IllegalStateException(
                    "Reservation cannot be checked in with current status: " + reservation.getStatus());
        }

        reservation.setStatus("CHECKED_IN");
        reservation.setCheckInTime(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    public TableReservation checkOut(Long reservationId) {
        TableReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.setStatus("COMPLETED");
        reservation.setCheckOutTime(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> getAvailableTables(Long restaurantId, LocalDate date, LocalTime time,
            Integer durationMinutes, Integer numberOfGuests) {
        List<RestaurantTable> allTables = getActiveTablesByRestaurant(restaurantId);

        return allTables.stream()
                .filter(table -> table.getCapacity() >= numberOfGuests)
                .filter(table -> isTableAvailable(table.getId(), date, time, durationMinutes))
                .collect(Collectors.toList());
    }

    private String generateQRCode(Long restaurantId, Long tableId) {
        // Generate unique QR code
        return String.format("RES-%d-%d-%s", restaurantId, tableId, UUID.randomUUID().toString().substring(0, 8));
    }

    public void autoReleaseExpiredReservations() {
        List<TableReservation> expired = reservationRepository.findReservationsToAutoRelease(LocalDateTime.now());
        for (TableReservation reservation : expired) {
            reservation.setStatus("CANCELLED");
            reservationRepository.save(reservation);
        }
    }

    public RestaurantTable createTable(RestaurantTable table) {
        // Check if table number already exists for this restaurant (including inactive
        // tables)
        Optional<RestaurantTable> existing = tableRepository.findByRestaurantAndTableNumber(
                table.getRestaurant(), table.getTableNumber());
        if (existing.isPresent()) {
            RestaurantTable existingTable = existing.get();
            if (existingTable.getIsActive()) {
                throw new IllegalStateException("Table number " + table.getTableNumber()
                        + " already exists for this restaurant. Please use a different table number.");
            } else {
                // If inactive, reactivate it instead of creating a duplicate
                existingTable.setIsActive(true);
                existingTable.setTableName(table.getTableName());
                existingTable.setCapacity(table.getCapacity());
                existingTable.setTableType(table.getTableType());
                existingTable.setFloorNumber(table.getFloorNumber());
                existingTable.setSectionName(table.getSectionName());
                return tableRepository.save(existingTable);
            }
        }
        return tableRepository.save(table);
    }

    public void deleteTable(Long tableId) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found"));

        // Check if table has active reservations
        List<TableReservation> activeReservations = reservationRepository.findByTableAndReservationDate(
                table, LocalDate.now());
        boolean hasActiveReservations = activeReservations.stream()
                .anyMatch(r -> r.getStatus().equals("PENDING") || r.getStatus().equals("CONFIRMED")
                        || r.getStatus().equals("CHECKED_IN"));

        if (hasActiveReservations) {
            throw new IllegalStateException("Cannot delete table with active reservations");
        }

        table.setIsActive(false);
        tableRepository.save(table);
    }

    public int createSampleTables(Restaurant restaurant) {
        int count = 0;
        String[] sections = { "Main Hall", "Garden", "VIP" };
        String[] tableTypes = { "STANDARD", "FAMILY", "VIP" };
        int[] capacities = { 2, 4, 6 };

        int tableNum = 1;
        int xPos = 50;
        int yPos = 50;

        // Main Hall - 6 tables
        for (int i = 0; i < 6; i++) {
            String tableNumber = "T" + tableNum++;
            if (tableRepository.findByRestaurantAndTableNumber(restaurant, tableNumber).isPresent()) {
                xPos += 120;
                if (i == 2) {
                    xPos = 50;
                    yPos += 120;
                }
                continue;
            }

            RestaurantTable table = new RestaurantTable();
            table.setRestaurant(restaurant);
            table.setTableNumber(tableNumber);
            table.setTableName("Table " + tableNumber);
            table.setCapacity(capacities[i % 3]);
            table.setTableType(tableTypes[i % 3]);
            table.setFloorNumber(1);
            table.setSectionName(sections[0]);
            table.setXPosition(xPos);
            table.setYPosition(yPos);
            table.setIsActive(true);
            tableRepository.save(table);
            count++;

            xPos += 120;
            if (i == 2) {
                xPos = 50;
                yPos += 120;
            }
        }

        // Garden - 4 tables
        xPos = 50;
        yPos = 300;
        for (int i = 0; i < 4; i++) {
            String tableNumber = "T" + tableNum++;
            if (tableRepository.findByRestaurantAndTableNumber(restaurant, tableNumber).isPresent()) {
                xPos += 120;
                if (i == 1) {
                    xPos = 50;
                    yPos += 120;
                }
                continue;
            }

            RestaurantTable table = new RestaurantTable();
            table.setRestaurant(restaurant);
            table.setTableNumber(tableNumber);
            table.setTableName("Table " + tableNumber);
            table.setCapacity(4);
            table.setTableType("STANDARD");
            table.setFloorNumber(1);
            table.setSectionName(sections[1]);
            table.setXPosition(xPos);
            table.setYPosition(yPos);
            table.setIsActive(true);
            tableRepository.save(table);
            count++;

            xPos += 120;
            if (i == 1) {
                xPos = 50;
                yPos += 120;
            }
        }

        // VIP - 2 tables
        xPos = 50;
        yPos = 550;
        for (int i = 0; i < 2; i++) {
            String tableNumber = "VIP" + (i + 1);
            if (tableRepository.findByRestaurantAndTableNumber(restaurant, tableNumber).isPresent()) {
                xPos += 150;
                continue;
            }

            RestaurantTable table = new RestaurantTable();
            table.setRestaurant(restaurant);
            table.setTableNumber(tableNumber);
            table.setTableName("VIP Table " + (i + 1));
            table.setCapacity(6);
            table.setTableType("VIP");
            table.setFloorNumber(1);
            table.setSectionName(sections[2]);
            table.setXPosition(xPos);
            table.setYPosition(yPos);
            table.setIsActive(true);
            tableRepository.save(table);
            count++;

            xPos += 150;
        }

        return count;
    }
}
