package com.srFoodDelivery.Controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.ArrayList;

import com.srFoodDelivery.dto.ReviewForm;
import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTag;
import com.srFoodDelivery.model.RestaurantTag;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.dto.order.MultiOrderCreateRequest;
import com.srFoodDelivery.dto.order.MultiOrderDTO;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.PreorderSlotRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.CartService;
import com.srFoodDelivery.service.EmailService;
import com.srFoodDelivery.service.MenuItemService;
import com.srFoodDelivery.service.MenuService;
import com.srFoodDelivery.service.OfferService;
import com.srFoodDelivery.service.OrderService;
import com.srFoodDelivery.service.PreorderService;
import com.srFoodDelivery.service.RestaurantService;
import com.srFoodDelivery.service.ReviewService;
import com.srFoodDelivery.service.SiteModeManager;
import com.srFoodDelivery.service.TableService;
import com.srFoodDelivery.service.order.OrderOrchestrationService;
import com.srFoodDelivery.model.PreorderSlot;
import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.SiteMode;
import com.srFoodDelivery.model.TableReservation;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final RestaurantService restaurantService;
    private final MenuService menuService;
    private final MenuItemService menuItemService;
    private final CartService cartService;
    private final ReviewService reviewService;
    private final OrderService orderService;
    private final OfferService offerService;
    private final PreorderService preorderService;
    private final TableService tableService;
    private final OrderOrchestrationService orderOrchestrationService;
    private final MultiOrderRepository multiOrderRepository;
    private final SiteModeManager siteModeManager;
    private final EmailService emailService;

    public CustomerController(RestaurantService restaurantService,
            MenuService menuService,
            MenuItemService menuItemService,
            CartService cartService,
            ReviewService reviewService,
            OrderService orderService,
            OfferService offerService,
            PreorderService preorderService,
            TableService tableService,
            OrderOrchestrationService orderOrchestrationService,
            MultiOrderRepository multiOrderRepository,
            SiteModeManager siteModeManager,
            EmailService emailService) {
        this.restaurantService = restaurantService;
        this.menuService = menuService;
        this.menuItemService = menuItemService;
        this.cartService = cartService;
        this.reviewService = reviewService;
        this.orderService = orderService;
        this.offerService = offerService;
        this.preorderService = preorderService;
        this.tableService = tableService;
        this.orderOrchestrationService = orderOrchestrationService;
        this.multiOrderRepository = multiOrderRepository;
        this.siteModeManager = siteModeManager;
        this.emailService = emailService;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return 0;
        }
        return cartService.getItemCount(principal.getUser());
    }

    @GetMapping({ "", "/restaurants" })
    public String browseRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "tags") List<String> tagParams,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String vegFilter,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {

        Map<String, String> availableTags = RestaurantTag.allTags();
        Set<String> selectedTags = tagParams != null ? new LinkedHashSet<>(tagParams) : new LinkedHashSet<>();
        boolean searchPresent = search != null && !search.trim().isEmpty();
        boolean categoryFilter = category != null && !category.trim().isEmpty();
        boolean vegFilterApplied = vegFilter != null && !vegFilter.trim().isEmpty();
        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        String modeLabel = siteMode.isCafeMode() ? "Cafe" : "Restaurant";

        // Always show items by default (like Swiggy), not restaurants
        List<MenuItem> items = menuItemService.getAvailableItemsForMode(siteMode);
        // Also get restaurants to show below items
        List<Restaurant> restaurants = restaurantService.findByMode(siteMode);
        boolean showingItems = true; // Always show items

        // Apply search filter
        if (searchPresent) {
            String searchLower = search.trim().toLowerCase();
            items = items.stream()
                    .filter(item -> item.getName().toLowerCase().contains(searchLower) ||
                            (item.getDescription() != null
                                    && item.getDescription().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        // Apply category filter
        if (categoryFilter) {
            items = items.stream()
                    .filter(item -> category.equalsIgnoreCase(item.getCategory()))
                    .collect(Collectors.toList());
        }

        // Apply tag filter
        if (!selectedTags.isEmpty()) {
            items = items.stream()
                    .filter(item -> {
                        Set<String> itemTags = item.getTags();
                        if (itemTags == null) {
                            itemTags = Collections.emptySet();
                        }
                        return itemTags.containsAll(selectedTags);
                    })
                    .collect(Collectors.toList());
        }

        // Apply veg/non-veg filter (always apply if specified)
        if (vegFilterApplied) {
            boolean isVeg = "veg".equalsIgnoreCase(vegFilter);
            items = items.stream()
                    .filter(item -> item.isVeg() == isVeg)
                    .collect(Collectors.toList());
        }

        // Get active offers
        List<com.srFoodDelivery.model.Offer> activeOffers = offerService.getActiveOffersForMode(siteMode);

        // Get all available categories
        List<String> categories = menuItemService.getAllCategoriesForMode(siteMode);

        model.addAttribute("availableTags", availableTags);
        model.addAttribute("selectedTags", selectedTags);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("category", category != null ? category : "");
        model.addAttribute("vegFilter", vegFilter != null ? vegFilter : "");
        model.addAttribute("showingItems", Boolean.valueOf(showingItems));
        model.addAttribute("items", items != null ? items : Collections.emptyList());
        model.addAttribute("itemCount", items != null ? items.size() : 0);
        model.addAttribute("restaurants", restaurants != null ? restaurants : Collections.emptyList());
        model.addAttribute("restaurantCount", restaurants != null ? restaurants.size() : 0);
        model.addAttribute("filtersApplied",
                Boolean.valueOf(showingItems && (!selectedTags.isEmpty() || searchPresent || categoryFilter)));
        model.addAttribute("searchApplied", Boolean.valueOf(searchPresent));
        model.addAttribute("searchPresent", Boolean.valueOf(searchPresent));
        model.addAttribute("activeOffers", activeOffers != null ? activeOffers : Collections.emptyList());
        model.addAttribute("categories", categories != null ? categories : Collections.emptyList());
        model.addAttribute("siteMode", siteMode);
        model.addAttribute("modeDisplayName", siteMode.getDisplayName());
        model.addAttribute("modeLabel", modeLabel);
        model.addAttribute("modeDescription", siteMode.getDescription());
        return "customer/restaurants";
    }

    @GetMapping("/offers")
    public String viewOffers(
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {
        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        List<com.srFoodDelivery.model.Offer> activeOffers = offerService.getActiveOffersForMode(siteMode);
        model.addAttribute("offers", activeOffers);
        model.addAttribute("siteMode", siteMode);
        model.addAttribute("modeDisplayName", siteMode.getDisplayName());
        model.addAttribute("modeDescription", siteMode.getDescription());
        return "customer/offers";
    }

    @GetMapping("/category/{categoryName}")
    public String viewCategory(
            @PathVariable String categoryName,
            @RequestParam(required = false) String vegFilter,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {

        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        List<MenuItem> items = menuItemService.getItemsByCategoryForMode(categoryName, siteMode);

        // Apply veg/non-veg filter if specified
        if (vegFilter != null && !vegFilter.trim().isEmpty()) {
            boolean isVeg = "veg".equalsIgnoreCase(vegFilter);
            items = items.stream()
                    .filter(item -> item.isVeg() == isVeg)
                    .collect(Collectors.toList());
        }

        // Group items by restaurant (filter out items with null restaurant)
        Map<Restaurant, List<MenuItem>> itemsByRestaurant = items.stream()
                .filter(item -> item.getMenu() != null && item.getMenu().getRestaurant() != null)
                .collect(Collectors.groupingBy(item -> item.getMenu().getRestaurant()));

        model.addAttribute("categoryName", categoryName);
        model.addAttribute("items", items);
        model.addAttribute("itemsByRestaurant", itemsByRestaurant);
        model.addAttribute("vegFilter", vegFilter != null ? vegFilter : "");
        model.addAttribute("siteMode", siteMode);
        model.addAttribute("modeDisplayName", siteMode.getDisplayName());
        model.addAttribute("modeDescription", siteMode.getDescription());
        return "customer/category";
    }

    @GetMapping("/restaurants/{id}")
    public String viewRestaurant(@PathVariable Long id, Model model) {
        Restaurant restaurant = restaurantService.getById(id);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menus", menuService.findByRestaurant(restaurant));
        return "customer/restaurant-detail";
    }

    @GetMapping("/menus/{id}")
    public String viewMenu(@PathVariable Long id,
            @RequestParam(required = false, name = "tags") List<String> tagParams,
            Model model) {
        Menu menu = menuService.getMenu(id);
        List<MenuItem> allItems = menuItemService.getItemsForMenu(id);
        Map<String, String> availableTags = RestaurantTag.allTags();
        Set<String> selectedTags = tagParams != null ? new HashSet<>(tagParams) : new HashSet<>();

        List<MenuItem> filteredItems = allItems;
        if (!selectedTags.isEmpty()) {
            filteredItems = allItems.stream()
                    .filter(item -> {
                        Set<String> itemTags = item.getTags();
                        if (itemTags == null) {
                            itemTags = Collections.emptySet();
                        }
                        return itemTags.containsAll(selectedTags);
                    })
                    .collect(Collectors.toList());
        }

        model.addAttribute("menu", menu);
        model.addAttribute("items", filteredItems);
        model.addAttribute("availableTags", availableTags);
        model.addAttribute("selectedTags", selectedTags);
        model.addAttribute("originalItemCount", allItems.size());
        return "customer/menu-detail";
    }

    @GetMapping("/restaurants/{id}/reviews")
    public String viewRestaurantReviews(@PathVariable Long id,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {
        Restaurant restaurant = restaurantService.getById(id);
        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("reviews", reviewService.getRestaurantReviews(restaurant));
        model.addAttribute("averageRating", reviewService.getAverageRating(restaurant));
        model.addAttribute("siteMode", siteMode);
        return "customer/restaurant-reviews";
    }

    @GetMapping("/restaurants/{id}/review")
    public String showReviewForm(@PathVariable Long id,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {
        Restaurant restaurant = restaurantService.getById(id);
        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        if (!model.containsAttribute("reviewForm")) {
            model.addAttribute("reviewForm", new ReviewForm());
        }
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("siteMode", siteMode);
        return "customer/review-form";
    }

    @PostMapping("/restaurants/{id}/review")
    public String submitReview(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("reviewForm") ReviewForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reviewForm",
                    bindingResult);
            redirectAttributes.addFlashAttribute("reviewForm", form);
            return "redirect:/customer/restaurants/" + id + "/review";
        }

        try {
            form.setRestaurantId(id);
            reviewService.createReview(principal.getUser(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Review submitted successfully");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/restaurants/" + id + "/review";
        }

        return "redirect:/customer/restaurants/" + id + "/reviews";
    }

    // Track order endpoint moved to CartController to handle both legacy orders and
    // MultiOrders
    // The endpoint /customer/orders/{orderId}/track is now handled by
    // CartController.trackMultiOrder()

    @GetMapping("/restaurants/{id}/preorder")
    public String showPreorderPage(@PathVariable Long id,
            @RequestParam(required = false) String step,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {
        Restaurant restaurant = restaurantService.getById(id);
        List<PreorderSlot> slots = preorderService.getAvailableSlotsByRestaurant(id);

        // Get all menu items for this restaurant through menus
        List<Menu> menus = menuService.findByRestaurant(restaurant);
        List<MenuItem> menuItems = menus.stream()
                .flatMap(menu -> menuItemService.getItemsForMenu(menu.getId()).stream())
                .filter(MenuItem::isAvailable)
                .collect(Collectors.toList());

        // Get tables for this restaurant
        List<RestaurantTable> tables = tableService.getActiveTablesByRestaurant(id);

        // Get unique sections and table types
        List<String> sections = tables.stream()
                .map(RestaurantTable::getSectionName)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        List<String> tableTypes = tables.stream()
                .map(RestaurantTable::getTableType)
                .distinct()
                .collect(Collectors.toList());

        String currentStep = step != null ? step : "1"; // Default to step 1

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("slots", slots);
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("tables", tables);
        model.addAttribute("sections", sections);
        model.addAttribute("tableTypes", tableTypes);
        model.addAttribute("currentStep", currentStep);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedTime", time);
        model.addAttribute("numberOfGuests", guests != null ? guests : 2);

        // Get active offers for this restaurant
        List<com.srFoodDelivery.model.Offer> activeOffers = offerService.getActiveOffersByRestaurant(restaurant);
        model.addAttribute("activeOffers", activeOffers);

        return "customer/preorder";
    }

    @PostMapping("/preorder/add-to-cart")
    public String addPreorderToCart(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Long slotId,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) String reservationDate,
            @RequestParam(required = false) String reservationTime,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) Integer numberOfGuests,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false) Long offerId,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Map<String, String[]> paramMap = request.getParameterMap();
        List<PreorderItemRequest> items = new ArrayList<>();

        // Parse items manually since relying on @RequestParam arrays might be flaky
        // with the JS form
        // JS sends: items[itemId] = quantity (from object iteration) OR distinct inputs
        // Wait, the new JS code sends:
        // const inputs = { ... }
        // for (const [name, value] of Object.entries(inputs)) ...
        // AND for items:
        // const items = []; items.push({ menuItemId: ..., quantity: ... })
        // AND THEN loop items to create inputs? No, let's allow the previous parsing
        // logic to work.
        // My previous JS edit:
        /*
         * for (const item of items) {
         * const idInput = document.createElement('input');
         * idInput.name = 'items[' + i + '][menuItemId]'; ...
         */
        // Actually, looking at the previous view_file, the JS iterates items and
        // creates hidden inputs?
        // No, I need to check how JS is creating item inputs.
        // Assuming the backend previously parsed arrays or Map, let's stick to the
        // existing "Parse items from arrays" logic
        // but since I can't easily see the hidden input structure for items in my JS
        // edit,
        // let's Assume the previous logical block works if I restore the signature.
        // BUT my REPLACE block below REPLACES the signature.
        // So I must include the array params if they were there OR map parsing.

        // Let's stick to the existing signature style plus new params.
        // And keep the existing parsing logic or use the map parsing I saw in a
        // previous turn (Step 225).
        // Actually, Step 231 shows it uses `String[] itemIds` and `String[]
        // quantities`.
        // So I will keep those.

        // Wait, the JS creates `items` array and appends... how?
        // Let's look at Step 219 JS again.
        // It creates `const items = []`.
        // It doesn't show the loop creating inputs.
        // I need to be careful. If the JS creates `itemIds` and `quantities` arrays,
        // fine.
        // If it creates `items[0][id]` then `itemIds` won't bind.

        // Let's implement robust parsing from the request map to be safe.

        for (String key : paramMap.keySet()) {
            if (key.startsWith("items[")) {
                try {
                    // Extract index
                    int startIndex = key.indexOf("[");
                    int endIndex = key.indexOf("]");
                    String indexStr = key.substring(startIndex + 1, endIndex);
                    int index = Integer.parseInt(indexStr);

                    while (items.size() <= index) {
                        items.add(new PreorderItemRequest());
                    }

                    PreorderItemRequest item = items.get(index);
                    if (key.contains("[menuItemId]")) {
                        item.setMenuItemId(Long.parseLong(paramMap.get(key)[0]));
                    } else if (key.contains("[quantity]")) {
                        item.setQuantity(Integer.parseInt(paramMap.get(key)[0]));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // Fallback to array params if list is empty
        if (items.isEmpty()) {
            String[] itemIds = request.getParameterValues("itemIds");
            String[] quantities = request.getParameterValues("quantities");
            if (itemIds != null && quantities != null && itemIds.length > 0 && itemIds.length == quantities.length) {
                for (int i = 0; i < itemIds.length; i++) {
                    try {
                        Long menuItemId = Long.parseLong(itemIds[i].trim());
                        Integer quantity = Integer.parseInt(quantities[i].trim());
                        if (quantity > 0) {
                            PreorderItemRequest item = new PreorderItemRequest();
                            item.setMenuItemId(menuItemId);
                            item.setQuantity(quantity);
                            items.add(item);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to continue");
            return "redirect:/login";
        }

        // Store coupon details
        if (couponCode != null && !couponCode.isEmpty()) {
            session.setAttribute("preorderCouponCode", couponCode);
        } else {
            session.removeAttribute("preorderCouponCode");
        }

        if (offerId != null) {
            session.setAttribute("preorderOfferId", offerId);
        } else {
            session.removeAttribute("preorderOfferId");
        }

        try {
            User user = principal.getUser();

            // Handle table reservation if table is selected
            Long finalReservationId = reservationId;
            if (tableId != null && reservationDate != null && reservationTime != null) {
                if (finalReservationId == null) {
                    // Create new reservation
                    java.time.LocalDate date = java.time.LocalDate.parse(reservationDate);
                    java.time.LocalTime time = java.time.LocalTime.parse(reservationTime);
                    int duration = durationMinutes != null ? durationMinutes : 60;
                    int guests = numberOfGuests != null ? numberOfGuests : 2;

                    TableReservation reservation = tableService.createReservation(
                            user, restaurantId, tableId, date, time, duration, guests, null);
                    finalReservationId = reservation.getId();
                }
                // Store reservation ID in session
                session.setAttribute("preorderReservationId", finalReservationId);
            }

            // Handle preorder slot if provided (for delivery preorders)
            if (slotId != null) {
                PreorderSlot slot = preorderService.getSlotById(slotId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid time slot selected"));

                if (slot.getCurrentCapacity() >= slot.getMaxCapacity()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "This time slot is fully booked. Please select another slot.");
                    return "redirect:/customer/restaurants/" + restaurantId + "/preorder";
                }

                session.setAttribute("preorderSlotId", slotId);
                redirectAttributes.addFlashAttribute("preorderSlotId", slotId);
            }

            // Store payment method preference
            if (paymentMethod != null) {
                session.setAttribute("preorderPaymentMethod", paymentMethod);
            }

            // Store reservation ID if exists
            if (finalReservationId != null) {
                session.setAttribute("preorderReservationId", finalReservationId);
            }

            // Store preorder items in session instead of adding to cart
            if (items != null && !items.isEmpty()) {
                // Store preorder items in session
                session.setAttribute("preorderItems", items);
                session.setAttribute("preorderRestaurantId", restaurantId);
                session.setAttribute("preorderReservationDate", reservationDate);
                session.setAttribute("preorderReservationTime", reservationTime);

                // Calculate total amount for preorder
                java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
                for (PreorderItemRequest item : items) {
                    if (item != null && item.getMenuItemId() != null && item.getQuantity() != null
                            && item.getQuantity() > 0) {
                        try {
                            com.srFoodDelivery.model.MenuItem menuItem = menuItemService
                                    .getMenuItem(item.getMenuItemId());
                            if (menuItem != null) {
                                totalAmount = totalAmount.add(
                                        menuItem.getPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
                            }
                        } catch (Exception e) {
                            // Skip invalid items
                        }
                    }
                }
                session.setAttribute("preorderTotalAmount", totalAmount);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one menu item.");
                return "redirect:/customer/restaurants/" + restaurantId + "/preorder";
            }

            // Redirect to preorder payment page
            return "redirect:/customer/preorder/payment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error adding items to cart: " + e.getMessage());
            return "redirect:/customer/restaurants/" + restaurantId + "/preorder";
        }
    }

    @GetMapping("/preorder/payment")
    public String preorderPaymentPage(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to continue");
            return "redirect:/login";
        }

        User user = principal.getUser();

        // Get preorder items from session
        @SuppressWarnings("unchecked")
        List<PreorderItemRequest> preorderItems = (List<PreorderItemRequest>) session.getAttribute("preorderItems");
        Long restaurantId = (Long) session.getAttribute("preorderRestaurantId");
        java.math.BigDecimal totalAmount = (java.math.BigDecimal) session.getAttribute("preorderTotalAmount");

        if (preorderItems == null || preorderItems.isEmpty() || restaurantId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Preorder session expired. Please try again.");
            return "redirect:/customer/restaurants/" + (restaurantId != null ? restaurantId : "");
        }

        // Get restaurant and menu items details
        Restaurant restaurant = restaurantService.getById(restaurantId);
        List<PreorderItemDetail> itemDetails = new java.util.ArrayList<>();
        java.math.BigDecimal calculatedTotal = java.math.BigDecimal.ZERO;

        for (PreorderItemRequest item : preorderItems) {
            if (item != null && item.getMenuItemId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                try {
                    MenuItem menuItem = menuItemService.getMenuItem(item.getMenuItemId());
                    if (menuItem != null) {
                        java.math.BigDecimal itemTotal = menuItem.getPrice()
                                .multiply(new java.math.BigDecimal(item.getQuantity()));
                        calculatedTotal = calculatedTotal.add(itemTotal);
                        itemDetails.add(new PreorderItemDetail(menuItem, item.getQuantity(), itemTotal));
                    }
                } catch (Exception e) {
                    // Skip invalid items
                }
            }
        }

        if (totalAmount == null) {
            totalAmount = calculatedTotal;
        }

        // Apply discount if coupon exists in session
        String couponCode = (String) session.getAttribute("preorderCouponCode");
        Long offerId = (Long) session.getAttribute("preorderOfferId");
        java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal originalTotal = totalAmount;

        if (offerId != null) {
            try {
                com.srFoodDelivery.model.Offer offer = offerService.getOfferById(offerId);

                if (offer != null && offer.isCurrentlyActive()) {
                    // Calculate discount
                    if ("PERCENTAGE_OFF".equals(offer.getOfferType())) {
                        discountAmount = totalAmount.multiply(offer.getDiscountValue())
                                .divide(new java.math.BigDecimal(100));
                        if (offer.getMaxDiscount() != null && discountAmount.compareTo(offer.getMaxDiscount()) > 0) {
                            discountAmount = offer.getMaxDiscount();
                        }
                    } else if ("FLAT_DISCOUNT".equals(offer.getOfferType())) {
                        discountAmount = offer.getDiscountValue();
                    }

                    // Ensure discount doesn't exceed total
                    if (discountAmount.compareTo(totalAmount) > 0) {
                        discountAmount = totalAmount;
                    }

                    // Round to 2 decimal places
                    discountAmount = discountAmount.setScale(2, java.math.RoundingMode.HALF_UP);

                    totalAmount = totalAmount.subtract(discountAmount);
                }
            } catch (Exception e) {
                // If offer invalid, ignore
            }
        }

        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("itemDetails", itemDetails);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("originalTotal", originalTotal);
        model.addAttribute("reservationId", session.getAttribute("preorderReservationId"));
        model.addAttribute("reservationDate", session.getAttribute("preorderReservationDate"));
        model.addAttribute("reservationTime", session.getAttribute("preorderReservationTime"));
        model.addAttribute("siteMode", siteMode);

        if (offerId != null && discountAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            model.addAttribute("couponCode", couponCode);
            model.addAttribute("offerId", offerId);
            model.addAttribute("discountAmount", discountAmount);
        }

        return "customer/preorder-payment";
    }

    @PostMapping("/preorder/payment/confirm")
    public String confirmPreorderPayment(@AuthenticationPrincipal CustomUserDetails principal,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam(name = "paymentOption", required = false) String paymentOption,
            @RequestParam(name = "upiId", required = false) String upiId,
            @RequestParam(name = "cardHolderName", required = false) String cardHolderName,
            @RequestParam(name = "cardNumber", required = false) String cardNumber,
            @RequestParam(name = "cardExpiry", required = false) String cardExpiry,
            @RequestParam(name = "cardCvv", required = false) String cardCvv) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to continue");
            return "redirect:/login";
        }

        User user = principal.getUser();

        // Get preorder items from session
        @SuppressWarnings("unchecked")
        List<PreorderItemRequest> preorderItems = (List<PreorderItemRequest>) session.getAttribute("preorderItems");
        Long restaurantId = (Long) session.getAttribute("preorderRestaurantId");
        Long reservationId = null;
        Object reservationIdObj = session.getAttribute("preorderReservationId");
        if (reservationIdObj instanceof Long) {
            reservationId = (Long) reservationIdObj;
        } else if (reservationIdObj instanceof String) {
            try {
                reservationId = Long.parseLong((String) reservationIdObj);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Get preorder slot ID from session
        Long preorderSlotId = null;
        Object slotIdObj = session.getAttribute("preorderSlotId");
        if (slotIdObj instanceof Long) {
            preorderSlotId = (Long) slotIdObj;
        } else if (slotIdObj instanceof String) {
            try {
                preorderSlotId = Long.parseLong((String) slotIdObj);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        if (preorderItems == null || preorderItems.isEmpty() || restaurantId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Preorder session expired. Please try again.");
            return "redirect:/customer/restaurants/" + restaurantId;
        }

        // Validate payment option
        String normalizedOption = paymentOption != null ? paymentOption.trim().toUpperCase() : "";
        if (normalizedOption.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a payment option.");
            return "redirect:/customer/preorder/payment";
        }

        try {
            // Add items to cart temporarily for order creation
            for (PreorderItemRequest item : preorderItems) {
                if (item != null && item.getMenuItemId() != null && item.getQuantity() != null
                        && item.getQuantity() > 0) {
                    cartService.addItem(user, item.getMenuItemId(), item.getQuantity());
                }
            }

            // Create order with preorder details
            // Use a dummy address for dine-in preorders (not needed for table reservations)
            String dummyAddress = "Dine-In Preorder - Table Reservation";
            orderService.placeOrder(user, dummyAddress, null, null, null, null, preorderSlotId, reservationId);

            // Update the latest order status after payment
            // For preorders, set to PENDING_APPROVAL (needs owner approval)
            // For delivery orders, set to CONFIRMED
            var latestOrders = orderOrchestrationService.getUserMultiOrders(user);
            if (!latestOrders.isEmpty()) {
                var latestOrderDTO = latestOrders.get(0); // Most recent order
                var multiOrderOpt = multiOrderRepository.findByIdAndUser(latestOrderDTO.getId(), user);
                if (multiOrderOpt.isPresent()) {
                    var multiOrder = multiOrderOpt.get();
                    if ("PENDING".equals(multiOrder.getStatus())) {
                        // Check if it's a preorder
                        boolean isPreorder = "PREORDER".equals(multiOrder.getOrderingMode()) ||
                                multiOrder.getSubOrders().stream()
                                        .anyMatch(so -> so.getPreorderSlot() != null);

                        if (isPreorder) {
                            // Preorders need owner approval
                            multiOrder.setStatus("PENDING_APPROVAL");
                        } else {
                            // Delivery orders are confirmed immediately
                            multiOrder.setStatus("CONFIRMED");
                        }
                        multiOrder.setPaymentStatus("PAID");
                        multiOrderRepository.save(multiOrder);

                        // Send email notification to customer
                        try {
                            emailService.sendMultiOrderConfirmationEmail(user, multiOrder);
                        } catch (Exception e) {
                            System.err.println("Failed to send preorder confirmation email: " + e.getMessage());
                        }
                    }
                }
            }

            // Clear preorder session
            session.removeAttribute("preorderItems");
            session.removeAttribute("preorderRestaurantId");
            session.removeAttribute("preorderTotalAmount");
            session.removeAttribute("preorderReservationId");
            session.removeAttribute("preorderReservationDate");
            session.removeAttribute("preorderReservationTime");
            session.removeAttribute("preorderSlotId");

            // Clear cart after order is placed
            cartService.clearCart(user);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Preorder successfully booked! Your table is reserved and your order will be prepared as scheduled.");
            return "redirect:/customer/preorder/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing preorder: " + e.getMessage());
            return "redirect:/customer/preorder/payment";
        }
    }

    @GetMapping("/preorder/success")
    public String preorderSuccess(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false, name = "mode") String mode,
            HttpSession session,
            Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        model.addAttribute("siteMode", siteMode);
        return "customer/preorder-success";
    }

    // Helper class for item details
    public static class PreorderItemDetail {
        private MenuItem menuItem;
        private Integer quantity;
        private java.math.BigDecimal total;

        public PreorderItemDetail(MenuItem menuItem, Integer quantity, java.math.BigDecimal total) {
            this.menuItem = menuItem;
            this.quantity = quantity;
            this.total = total;
        }

        public MenuItem getMenuItem() {
            return menuItem;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public java.math.BigDecimal getTotal() {
            return total;
        }
    }

    // Inner class for request binding
    public static class PreorderItemRequest {
        private Long menuItemId;
        private Integer quantity;

        public Long getMenuItemId() {
            return menuItemId;
        }

        public void setMenuItemId(Long menuItemId) {
            this.menuItemId = menuItemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    // Wrapper class for form binding
    public static class PreorderCartRequest {
        private List<PreorderItemRequest> items;

        public List<PreorderItemRequest> getItems() {
            return items;
        }

        public void setItems(List<PreorderItemRequest> items) {
            this.items = items;
        }
    }
}
