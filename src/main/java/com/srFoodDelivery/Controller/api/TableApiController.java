package com.srFoodDelivery.Controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.RestaurantTableRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.RestaurantService;
import com.srFoodDelivery.service.TableService;

@RestController
@RequestMapping("/api/tables")
public class TableApiController {

    private final TableService tableService;
    private final RestaurantService restaurantService;
    private final RestaurantTableRepository tableRepository;

    public TableApiController(TableService tableService,
            RestaurantService restaurantService,
            RestaurantTableRepository tableRepository) {
        this.tableService = tableService;
        this.restaurantService = restaurantService;
        this.tableRepository = tableRepository;
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<Map<String, Object>> getTablesByRestaurant(@PathVariable Long restaurantId) {
        List<RestaurantTable> tables = tableService.getActiveTablesByRestaurant(restaurantId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tables", tables);
        response.put("count", tables.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurant/{restaurantId}/create-sample")
    public ResponseEntity<Map<String, Object>> createSampleTables(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Restaurant restaurant = restaurantService.getById(restaurantId);
        if (restaurant == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Restaurant not found");
            return ResponseEntity.badRequest().body(error);
        }

        // Check if user is restaurant owner or admin
        if (principal != null) {
            User user = principal.getUser();
            if (!user.getRole().equals("ADMIN") &&
                    !restaurant.getOwner().getId().equals(user.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Unauthorized: Only restaurant owner or admin can add tables");
                return ResponseEntity.status(403).body(error);
            }
        }

        // Create sample tables
        int tablesCreated = createSampleTablesForRestaurant(restaurant);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Created " + tablesCreated + " sample tables");
        response.put("restaurantId", restaurantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurant/{restaurantId}/create")
    public ResponseEntity<Map<String, Object>> createTable(
            @PathVariable Long restaurantId,
            @RequestBody Map<String, Object> tableData,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Restaurant restaurant = restaurantService.getById(restaurantId);
        if (restaurant == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Restaurant not found");
            return ResponseEntity.badRequest().body(error);
        }

        // Check authorization
        if (principal != null) {
            User user = principal.getUser();
            if (!user.getRole().equals("ADMIN") &&
                    !restaurant.getOwner().getId().equals(user.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Unauthorized");
                return ResponseEntity.status(403).body(error);
            }
        }

        RestaurantTable table = new RestaurantTable();
        table.setRestaurant(restaurant);
        table.setTableNumber((String) tableData.getOrDefault("tableNumber", "T1"));
        table.setTableName((String) tableData.get("tableName"));
        table.setCapacity(((Number) tableData.getOrDefault("capacity", 2)).intValue());
        table.setTableType((String) tableData.getOrDefault("tableType", "STANDARD"));
        table.setFloorNumber(((Number) tableData.getOrDefault("floorNumber", 1)).intValue());
        table.setSectionName((String) tableData.get("sectionName"));
        table.setXPosition(
                tableData.get("xPosition") != null ? ((Number) tableData.get("xPosition")).intValue() : null);
        table.setYPosition(
                tableData.get("yPosition") != null ? ((Number) tableData.get("yPosition")).intValue() : null);
        table.setIsActive(true);

        tableRepository.save(table);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Table created successfully");
        response.put("table", table);
        return ResponseEntity.ok(response);
    }

    private int createSampleTablesForRestaurant(Restaurant restaurant) {
        int count = 0;

        // Create tables for different sections
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
                // Adjust position for next iteration even if skipped
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
            String tableNumber = "T" + tableNum++;
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
