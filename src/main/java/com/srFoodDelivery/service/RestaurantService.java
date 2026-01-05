package com.srFoodDelivery.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.RestaurantForm;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.SiteMode;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.CartRepository;
import com.srFoodDelivery.repository.DonationRepository;
import com.srFoodDelivery.repository.OfferRepository;
import com.srFoodDelivery.repository.OrderRepository;
import com.srFoodDelivery.repository.PreorderSlotRepository;
import com.srFoodDelivery.repository.RatingReviewRepository;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.RestaurantReviewRepository;
import com.srFoodDelivery.repository.RestaurantRiderRepository;
import com.srFoodDelivery.repository.RestaurantTableRepository;
import com.srFoodDelivery.repository.ReviewRepository;
import com.srFoodDelivery.repository.RiderOfferRepository;
import com.srFoodDelivery.repository.SubOrderItemRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.repository.TableReservationRepository;

@Service
@Transactional(readOnly = true)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final ImageService imageService;
    private final RestaurantTableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final SubOrderItemRepository subOrderItemRepository;
    private final RiderOfferRepository riderOfferRepository;
    private final OfferRepository offerRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final RatingReviewRepository ratingReviewRepository;
    private final ReviewRepository reviewRepository;
    private final TableReservationRepository tableReservationRepository;
    private final PreorderSlotRepository preorderSlotRepository;
    private final RestaurantRiderRepository restaurantRiderRepository;
    private final DonationRepository donationRepository;
    private final CartRepository cartRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, ImageService imageService,
                             RestaurantTableRepository tableRepository,
                             OrderRepository orderRepository,
                             SubOrderRepository subOrderRepository,
                             SubOrderItemRepository subOrderItemRepository,
                             RiderOfferRepository riderOfferRepository,
                             OfferRepository offerRepository,
                             RestaurantReviewRepository restaurantReviewRepository,
                             RatingReviewRepository ratingReviewRepository,
                             ReviewRepository reviewRepository,
                             TableReservationRepository tableReservationRepository,
                             PreorderSlotRepository preorderSlotRepository,
                             RestaurantRiderRepository restaurantRiderRepository,
                             DonationRepository donationRepository,
                             CartRepository cartRepository) {
        this.restaurantRepository = restaurantRepository;
        this.imageService = imageService;
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.subOrderRepository = subOrderRepository;
        this.subOrderItemRepository = subOrderItemRepository;
        this.riderOfferRepository = riderOfferRepository;
        this.offerRepository = offerRepository;
        this.restaurantReviewRepository = restaurantReviewRepository;
        this.ratingReviewRepository = ratingReviewRepository;
        this.reviewRepository = reviewRepository;
        this.tableReservationRepository = tableReservationRepository;
        this.preorderSlotRepository = preorderSlotRepository;
        this.restaurantRiderRepository = restaurantRiderRepository;
        this.donationRepository = donationRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public Restaurant createRestaurant(RestaurantForm form, User owner) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(form.getName());
        restaurant.setDescription(form.getDescription());
        restaurant.setAddress(form.getAddress());
        restaurant.setContactNumber(form.getContactNumber());
        
        // Auto-generate image URL if not provided
        if (form.getImageUrl() == null || form.getImageUrl().trim().isEmpty()) {
            restaurant.setImageUrl(imageService.getImageUrlForRestaurant(form.getName()));
        } else {
            restaurant.setImageUrl(form.getImageUrl());
        }
        
        restaurant.setOpeningTime(form.getOpeningTime());
        restaurant.setClosingTime(form.getClosingTime());
        restaurant.setOwner(owner);

        boolean isCafe = "CAFE".equalsIgnoreCase(form.getBusinessType());
        restaurant.setCafeLounge(isCafe);
        restaurant.setFamilyRestaurant(!isCafe);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        
        // Create tables if dine-in is enabled and number of tables is specified
        if (form.getHasDineIn() != null && form.getHasDineIn() && 
            form.getNumberOfTables() != null && form.getNumberOfTables() > 0) {
            createTablesForRestaurant(savedRestaurant, form);
        }
        
        return savedRestaurant;
    }
    
    private void createTablesForRestaurant(Restaurant restaurant, RestaurantForm form) {
        int numberOfTables = form.getNumberOfTables();
        int defaultCapacity = form.getDefaultTableCapacity() != null ? form.getDefaultTableCapacity() : 4;
        String layout = form.getTableLayout() != null ? form.getTableLayout() : "GRID";
        int tablesPerRow = form.getTablesPerRow() != null ? form.getTablesPerRow() : 3;
        
        int xPos = 50;
        int yPos = 50;
        int tableNum = 1;
        int currentRow = 0;
        
        for (int i = 0; i < numberOfTables; i++) {
            RestaurantTable table = new RestaurantTable();
            table.setRestaurant(restaurant);
            table.setTableNumber("T" + tableNum++);
            table.setTableName("Table " + table.getTableNumber());
            table.setCapacity(defaultCapacity);
            table.setTableType(determineTableType(defaultCapacity));
            table.setFloorNumber(1);
            table.setSectionName("Main Hall");
            table.setXPosition(xPos);
            table.setYPosition(yPos);
            table.setIsActive(true);
            tableRepository.save(table);
            
            // Update position for next table
            if (layout.equals("GRID")) {
                xPos += 120;
                if ((i + 1) % tablesPerRow == 0) {
                    xPos = 50;
                    yPos += 120;
                    currentRow++;
                }
            } else {
                // Simple linear layout
                xPos += 120;
                if (xPos > 600) {
                    xPos = 50;
                    yPos += 120;
                }
            }
        }
    }
    
    private String determineTableType(int capacity) {
        if (capacity <= 2) {
            return "STANDARD";
        } else if (capacity <= 4) {
            return "STANDARD";
        } else if (capacity <= 6) {
            return "FAMILY";
        } else {
            return "VIP";
        }
    }

    public Restaurant getOwnedRestaurant(Long id, User owner) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Access denied");
        }
        ensureImageUrl(restaurant);
        return restaurant;
    }
    
    /**
     * Get restaurant for editing without calling ensureImageUrl
     * This preserves the user's custom imageUrl
     */
    public Restaurant getRestaurantForEdit(Long id, User owner) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Access denied");
        }
        // Don't call ensureImageUrl here - preserve whatever is in the database
        return restaurant;
    }

    @Transactional
    public Restaurant updateRestaurant(Long id, RestaurantForm form, User owner) {
        // Get restaurant without calling ensureImageUrl to avoid overwriting user's input
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        if (!restaurant.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Access denied");
        }
        
        restaurant.setName(form.getName());
        restaurant.setDescription(form.getDescription());
        restaurant.setAddress(form.getAddress());
        restaurant.setContactNumber(form.getContactNumber());
        
        // Handle imageUrl: Always use form's value if provided, otherwise keep existing
        String formImageUrl = form.getImageUrl();
        if (formImageUrl != null && !formImageUrl.trim().isEmpty()) {
            // User provided a URL in the form - use it (trimmed)
            String trimmedUrl = formImageUrl.trim();
            restaurant.setImageUrl(trimmedUrl);
            System.out.println("DEBUG: Setting imageUrl from form: " + trimmedUrl);
        } else {
            // Form field is empty - keep existing URL if it exists, otherwise auto-generate
            if (restaurant.getImageUrl() == null || restaurant.getImageUrl().trim().isEmpty()) {
                restaurant.setImageUrl(imageService.getImageUrlForRestaurant(form.getName()));
                System.out.println("DEBUG: Auto-generating imageUrl");
            } else {
                System.out.println("DEBUG: Keeping existing imageUrl: " + restaurant.getImageUrl());
            }
        }
        
        restaurant.setOpeningTime(form.getOpeningTime());
        restaurant.setClosingTime(form.getClosingTime());

        boolean isCafe = "CAFE".equalsIgnoreCase(form.getBusinessType());
        restaurant.setCafeLounge(isCafe);
        restaurant.setFamilyRestaurant(!isCafe);
        
        Restaurant saved = restaurantRepository.save(restaurant);
        restaurantRepository.flush();
        
        System.out.println("DEBUG: Saved restaurant ID " + saved.getId() + " with imageUrl: " + saved.getImageUrl());
        return saved;
    }

    @Transactional
    public void deleteRestaurant(Long id, User owner) {
        Restaurant restaurant = getOwnedRestaurant(id, owner);
        deleteRestaurantAndRelatedEntities(restaurant);
    }

    @Transactional
    public void deleteRestaurantByName(String name) {
        List<Restaurant> restaurants = restaurantRepository.findByNameIgnoreCase(name);
        if (restaurants.isEmpty()) {
            throw new IllegalArgumentException("Restaurant with name '" + name + "' not found");
        }
        // Delete all restaurants with this name (should typically be just one)
        for (Restaurant restaurant : restaurants) {
            deleteRestaurantAndRelatedEntities(restaurant);
        }
    }

    /**
     * Deletes a restaurant and all related entities to avoid foreign key constraint violations.
     * Order of deletion is important to respect foreign key constraints.
     */
    private void deleteRestaurantAndRelatedEntities(Restaurant restaurant) {
        Long restaurantId = restaurant.getId();
        
        // 1. Delete RiderOffers for SubOrders of this restaurant
        List<SubOrder> subOrders = subOrderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
        for (SubOrder subOrder : subOrders) {
            riderOfferRepository.deleteAll(riderOfferRepository.findBySubOrderOrderByCreatedAtDesc(subOrder));
            subOrderItemRepository.deleteAll(subOrderItemRepository.findBySubOrder(subOrder));
        }
        
        // 2. Delete SubOrders for this restaurant
        subOrderRepository.deleteAll(subOrders);
        
        // 3. Delete Orders (old system) for this restaurant
        orderRepository.deleteAll(orderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant));
        
        // 4. Delete Offers for this restaurant
        offerRepository.deleteAll(offerRepository.findByRestaurantOrderByCreatedAtDesc(restaurant));
        
        // 5. Delete Reviews for this restaurant
        restaurantReviewRepository.deleteAll(restaurantReviewRepository.findByRestaurantOrderByCreatedAtDesc(restaurant));
        ratingReviewRepository.deleteAll(ratingReviewRepository.findByRestaurantOrderByCreatedAtDesc(restaurant));
        reviewRepository.deleteAll(reviewRepository.findByRestaurantOrderByCreatedAtDesc(restaurant));
        
        // 6. Delete TableReservations for this restaurant
        // Delete reservations by date range (past and future)
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = -365; i <= 365; i++) { // Check 1 year back and 1 year ahead
            java.time.LocalDate date = today.plusDays(i);
            tableReservationRepository.deleteAll(tableReservationRepository.findByRestaurantAndReservationDate(restaurant, date));
        }
        
        // 7. Delete RestaurantTables for this restaurant
        // Get all tables (active and inactive) by filtering from all tables
        List<RestaurantTable> allTables = tableRepository.findAll().stream()
                .filter(t -> t.getRestaurant() != null && t.getRestaurant().getId().equals(restaurantId))
                .collect(java.util.stream.Collectors.toList());
        tableRepository.deleteAll(allTables);
        
        // 8. Delete PreorderSlots for this restaurant
        preorderSlotRepository.deleteAll(preorderSlotRepository.findByRestaurantAndIsActiveTrueOrderBySlotStartTimeAsc(restaurant));
        
        // 9. Delete RestaurantRiders for this restaurant
        restaurantRiderRepository.deleteAll(restaurantRiderRepository.findByRestaurantAndIsActiveTrue(restaurant));
        
        // 10. Delete Donations for this restaurant
        donationRepository.deleteAll(donationRepository.findByRestaurantOrderByCreatedAtDesc(restaurant));
        
        // 11. Delete Carts that reference this restaurant
        // Note: CartRepository doesn't have findByRestaurant, but we can check if needed
        // Carts are typically user-specific, so we'll skip this to avoid deleting user carts
        
        // 12. Finally, delete the restaurant itself
        // Menus and MenuItems will be deleted via cascade (Restaurant has @OneToMany with cascade = CascadeType.ALL)
        restaurantRepository.delete(restaurant);
    }

    public List<Restaurant> findByOwner(User owner) {
        List<Restaurant> restaurants = restaurantRepository.findByOwner(owner);
        ensureImageUrls(restaurants);
        return restaurants;
    }

    public List<Restaurant> findAll() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        ensureImageUrls(restaurants);
        return restaurants;
    }

    public List<Restaurant> findByMode(SiteMode siteMode) {
        List<Restaurant> restaurants;
        if (siteMode != null && siteMode.isCafeMode()) {
            // Cafe mode: show only cafes
            restaurants = restaurantRepository.findByIsActiveTrueAndIsCafeLoungeTrue();
        } else {
            // Restaurant mode (or null): show only non-cafe restaurants
            restaurants = restaurantRepository.findByIsActiveTrue().stream()
                    .filter(r -> !r.isCafeLounge())
                    .collect(java.util.stream.Collectors.toList());
        }
        ensureImageUrls(restaurants);
        return restaurants;
    }

    public Restaurant getById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        ensureImageUrl(restaurant);
        return restaurant;
    }
    
    private void ensureImageUrls(List<Restaurant> restaurants) {
        for (Restaurant restaurant : restaurants) {
            ensureImageUrl(restaurant);
        }
    }
    
    private void ensureImageUrl(Restaurant restaurant) {
        // Only auto-generate if imageUrl is truly null or empty (after trimming)
        // Don't overwrite user-provided URLs
        String currentUrl = restaurant.getImageUrl();
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            restaurant.setImageUrl(imageService.getImageUrlForRestaurant(restaurant.getName()));
        }
    }
}
