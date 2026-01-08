package com.srFoodDelivery.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.srFoodDelivery.model.RestaurantTag;

import com.srFoodDelivery.dto.DonationForm;
import com.srFoodDelivery.dto.MenuForm;
import com.srFoodDelivery.dto.MenuItemForm;
import com.srFoodDelivery.dto.RestaurantForm;
import com.srFoodDelivery.dto.SubscriptionPackageForm;
import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTable;
import com.srFoodDelivery.model.SubscriptionPackage;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.CompanySubscription;
import com.srFoodDelivery.model.CompanyOrder;
import com.srFoodDelivery.model.Offer;
import com.srFoodDelivery.security.CustomUserDetails;

import com.srFoodDelivery.model.OrderStatus;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.PreorderSlotRepository;
import com.srFoodDelivery.repository.SubscriptionPackageRepository;
import com.srFoodDelivery.repository.CompanySubscriptionRepository;
import com.srFoodDelivery.repository.CompanyOrderRepository;
import com.srFoodDelivery.service.DonationService;
import com.srFoodDelivery.service.EmailService;
import com.srFoodDelivery.service.MenuItemService;
import com.srFoodDelivery.service.MenuService;
import com.srFoodDelivery.service.OrderService;
import com.srFoodDelivery.service.RestaurantService;
import com.srFoodDelivery.service.SubscriptionPackageService;
import com.srFoodDelivery.service.TableService;
import com.srFoodDelivery.service.CompanyService;
import com.srFoodDelivery.websocket.OrderWebSocketPublisher;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final RestaurantService restaurantService;
    private final MenuService menuService;
    private final MenuItemService menuItemService;
    private final DonationService donationService;
    private final OrderService orderService;
    private final SubOrderRepository subOrderRepository;
    private final MultiOrderRepository multiOrderRepository;
    private final PreorderSlotRepository preorderSlotRepository;
    private final OrderWebSocketPublisher webSocketPublisher;
    private final TableService tableService;
    private final EmailService emailService;
    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final SubscriptionPackageService subscriptionPackageService;
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final CompanyService companyService;
    private final CompanyOrderRepository companyOrderRepository;
    private final com.srFoodDelivery.service.OfferService offerService;

    public OwnerController(RestaurantService restaurantService,
            MenuService menuService,
            MenuItemService menuItemService,
            DonationService donationService,
            OrderService orderService,
            SubOrderRepository subOrderRepository,
            MultiOrderRepository multiOrderRepository,
            PreorderSlotRepository preorderSlotRepository,
            OrderWebSocketPublisher webSocketPublisher,
            TableService tableService,
            EmailService emailService,
            SubscriptionPackageRepository subscriptionPackageRepository,
            SubscriptionPackageService subscriptionPackageService,
            CompanySubscriptionRepository companySubscriptionRepository,
            CompanyService companyService,
            CompanyOrderRepository companyOrderRepository,
            com.srFoodDelivery.service.OfferService offerService) {
        this.restaurantService = restaurantService;
        this.menuService = menuService;
        this.menuItemService = menuItemService;
        this.donationService = donationService;
        this.orderService = orderService;
        this.subOrderRepository = subOrderRepository;
        this.multiOrderRepository = multiOrderRepository;
        this.preorderSlotRepository = preorderSlotRepository;
        this.webSocketPublisher = webSocketPublisher;
        this.tableService = tableService;
        this.emailService = emailService;
        this.subscriptionPackageRepository = subscriptionPackageRepository;
        this.subscriptionPackageService = subscriptionPackageService;
        this.companySubscriptionRepository = companySubscriptionRepository;
        this.companyService = companyService;
        this.companyOrderRepository = companyOrderRepository;
        this.offerService = offerService;
    }

    @GetMapping({ "", "/dashboard" })
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User owner = principal.getUser();
        List<Restaurant> restaurants = restaurantService.findByOwner(owner);
        int menuCount = restaurants.stream()
                .map(Restaurant::getMenus)
                .filter(menus -> menus != null)
                .mapToInt(List::size)
                .sum();

        model.addAttribute("owner", owner);
        model.addAttribute("restaurants", restaurants);
        model.addAttribute("menuCount", menuCount);
        return "owner/dashboard";
    }

    @GetMapping("/restaurants/new")
    public String newRestaurant(Model model) {
        if (!model.containsAttribute("restaurantForm")) {
            model.addAttribute("restaurantForm", new RestaurantForm());
        }
        return "owner/restaurant-form";
    }

    @PostMapping("/restaurants")
    public String createRestaurant(@AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("restaurantForm") RestaurantForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "owner/restaurant-form";
        }

        restaurantService.createRestaurant(form, principal.getUser());
        return "redirect:/owner/dashboard";
    }

    @GetMapping("/restaurants/{id}/edit")
    public String editRestaurant(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        // Get restaurant directly without calling ensureImageUrl to preserve user's
        // imageUrl
        Restaurant restaurant = restaurantService.getRestaurantForEdit(id, principal.getUser());
        model.addAttribute("restaurantId", restaurant.getId());
        model.addAttribute("restaurantForm", toRestaurantForm(restaurant));
        return "owner/restaurant-form";
    }

    @PostMapping("/restaurants/{id}/edit")
    public String updateRestaurant(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("restaurantForm") RestaurantForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("restaurantId", id);
            return "owner/restaurant-form";
        }

        restaurantService.updateRestaurant(id, form, principal.getUser());
        return "redirect:/owner/dashboard";
    }

    @PostMapping("/restaurants/{id}/delete")
    public String deleteRestaurant(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        restaurantService.deleteRestaurant(id, principal.getUser());
        return "redirect:/owner/dashboard";
    }

    // Table Management Endpoints
    @GetMapping("/restaurants/{id}/tables")
    public String manageTables(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        List<RestaurantTable> tables = tableService.getActiveTablesByRestaurant(id);

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("tables", tables);
        return "owner/tables";
    }

    @PostMapping("/restaurants/{id}/tables/create")
    public String createTable(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("tableNumber") String tableNumber,
            @RequestParam("tableName") String tableName,
            @RequestParam("capacity") Integer capacity,
            @RequestParam("tableType") String tableType,
            @RequestParam(value = "floorNumber", required = false, defaultValue = "1") Integer floorNumber,
            @RequestParam(value = "sectionName", required = false) String sectionName,
            RedirectAttributes redirectAttributes) {
        try {
            Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());

            RestaurantTable table = new RestaurantTable();
            table.setRestaurant(restaurant);
            table.setTableNumber(tableNumber);
            table.setTableName(tableName);
            table.setCapacity(capacity);
            table.setTableType(tableType);
            table.setFloorNumber(floorNumber);
            table.setSectionName(sectionName);
            table.setIsActive(true);

            tableService.createTable(table);
            redirectAttributes.addFlashAttribute("successMessage", "Table created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating table: " + e.getMessage());
        }
        return "redirect:/owner/restaurants/" + id + "/tables";
    }

    @PostMapping("/restaurants/{id}/tables/{tableId}/delete")
    public String deleteTable(@PathVariable Long id,
            @PathVariable Long tableId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        try {
            restaurantService.getOwnedRestaurant(id, principal.getUser()); // Verify ownership
            tableService.deleteTable(tableId);
            redirectAttributes.addFlashAttribute("successMessage", "Table deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting table: " + e.getMessage());
        }
        return "redirect:/owner/restaurants/" + id + "/tables";
    }

    @PostMapping("/restaurants/{id}/tables/create-sample")
    public String createSampleTables(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        try {
            Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
            int count = tableService.createSampleTables(restaurant);
            redirectAttributes.addFlashAttribute("successMessage", "Created " + count + " sample tables!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating tables: " + e.getMessage());
        }
        return "redirect:/owner/restaurants/" + id + "/tables";
    }

    // Continue with existing methods...
    @GetMapping("/restaurants/{id}/menus")
    public String viewMenus(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        List<Menu> menus = menuService.findByRestaurant(restaurant);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menus", menus);
        return "owner/menus";
    }

    @GetMapping("/restaurants/{id}/menus/new")
    public String newMenu(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        if (!model.containsAttribute("menuForm")) {
            MenuForm form = new MenuForm();
            form.setRestaurantId(id);
            form.setType(Menu.Type.RESTAURANT.name());
            model.addAttribute("menuForm", form);
        }
        model.addAttribute("restaurant", restaurant);
        addMenuItemTagOptions(model);
        return "owner/menu-form";
    }

    @PostMapping("/restaurants/{id}/menus")
    public String createMenu(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("menuForm") MenuForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
            model.addAttribute("restaurant", restaurant);
            addMenuItemTagOptions(model);
            return "owner/menu-form";
        }

        menuService.createMenu(form);
        return "redirect:/owner/restaurants/" + id + "/menus";
    }

    @GetMapping("/restaurants/{restaurantId}/menus/{menuId}/edit")
    public String editMenu(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        requireOwnedMenu(menuId, principal.getUser());
        Menu menu = menuService.getMenu(menuId);
        model.addAttribute("menuForm", toMenuForm(menu));
        model.addAttribute("restaurant", menu.getRestaurant());
        addMenuItemTagOptions(model);
        return "owner/menu-form";
    }

    @PostMapping("/restaurants/{restaurantId}/menus/{menuId}/edit")
    public String updateMenu(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("menuForm") MenuForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            Menu menu = menuService.getMenu(menuId);
            model.addAttribute("restaurant", menu.getRestaurant());
            addMenuItemTagOptions(model);
            return "owner/menu-form";
        }

        menuService.updateMenu(menuId, form);
        return "redirect:/owner/restaurants/" + restaurantId + "/menus";
    }

    @PostMapping("/restaurants/{restaurantId}/menus/{menuId}/delete")
    public String deleteMenu(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        menuService.deleteMenu(menuId);
        return "redirect:/owner/restaurants/" + restaurantId + "/menus";
    }

    @GetMapping("/restaurants/{restaurantId}/menus/{menuId}/items")
    public String viewMenuItems(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        requireOwnedMenu(menuId, principal.getUser());
        Menu menu = menuService.getMenu(menuId);
        List<MenuItem> items = menuItemService.getItemsForMenu(menuId);
        model.addAttribute("restaurant", menu.getRestaurant());
        model.addAttribute("menu", menu);
        model.addAttribute("items", items);
        addMenuItemTagOptions(model);
        return "owner/menu-items";
    }

    @GetMapping("/restaurants/{restaurantId}/menus/{menuId}/items/new")
    public String newMenuItem(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        requireOwnedMenu(menuId, principal.getUser());
        Menu menu = menuService.getMenu(menuId);
        if (!model.containsAttribute("menuItemForm")) {
            model.addAttribute("menuItemForm", new MenuItemForm());
        }
        model.addAttribute("restaurant", menu.getRestaurant());
        model.addAttribute("menu", menu);
        addMenuItemTagOptions(model);
        return "owner/menu-item-form";
    }

    @PostMapping("/restaurants/{restaurantId}/menus/{menuId}/items")
    public String createMenuItem(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("menuItemForm") MenuItemForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            Menu menu = menuService.getMenu(menuId);
            model.addAttribute("restaurant", menu.getRestaurant());
            model.addAttribute("menu", menu);
            addMenuItemTagOptions(model);
            return "owner/menu-item-form";
        }

        menuItemService.addMenuItem(menuId, form);
        return "redirect:/owner/restaurants/" + restaurantId + "/menus/" + menuId + "/items";
    }

    @GetMapping("/restaurants/{restaurantId}/menus/{menuId}/items/{itemId}/edit")
    public String editMenuItem(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        requireOwnedMenuItem(itemId, principal.getUser());
        MenuItem item = menuItemService.getMenuItemForEdit(itemId);
        Menu menu = menuService.getMenu(menuId);
        model.addAttribute("menuItemForm", toMenuItemForm(item));
        model.addAttribute("restaurant", menu.getRestaurant());
        model.addAttribute("menu", menu);
        addMenuItemTagOptions(model);
        return "owner/menu-item-form";
    }

    @PostMapping("/restaurants/{restaurantId}/menus/{menuId}/items/{itemId}/edit")
    public String updateMenuItem(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("menuItemForm") MenuItemForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            MenuItem item = menuItemService.getMenuItemForEdit(itemId);
            Menu menu = item.getMenu();
            model.addAttribute("restaurant", menu.getRestaurant());
            model.addAttribute("menu", menu);
            addMenuItemTagOptions(model);
            return "owner/menu-item-form";
        }

        menuItemService.updateMenuItem(itemId, form);
        return "redirect:/owner/restaurants/" + restaurantId + "/menus/" + menuId + "/items";
    }

    @PostMapping("/restaurants/{restaurantId}/menus/{menuId}/items/{itemId}/delete")
    public String deleteMenuItem(@PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        menuItemService.deleteMenuItem(itemId);
        return "redirect:/owner/restaurants/" + restaurantId + "/menus/" + menuId + "/items";
    }

    @GetMapping("/restaurants/{id}/orders")
    public String viewOrders(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());

        List<SubOrder> deliveryOrders = subOrderRepository.findDeliveryOrders(restaurant);
        List<SubOrder> preorders = subOrderRepository.findPreorders(restaurant);
        List<CompanyOrder> companyOrders = companyOrderRepository
                .findByMenuItem_Menu_Restaurant_IdOrderByOrderDateDesc(restaurant.getId());

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("deliveryOrders", deliveryOrders);
        model.addAttribute("preorders", preorders);
        model.addAttribute("companyOrders", companyOrders);
        return "owner/orders";
    }

    @PostMapping("/restaurants/{id}/orders/{orderId}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
            @PathVariable Long orderId,
            @RequestParam("status") String status,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        SubOrder subOrder = subOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!subOrder.getRestaurant().getId().equals(restaurant.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Validate status transition
        String currentStatus = subOrder.getStatus();

        // Block status updates for preorders (except cancellation/rejection which are
        // handled separately)
        if (subOrder.getPreorderSlot() != null || (subOrder.getMultiOrder() != null
                && ("PREORDER".equals(subOrder.getMultiOrder().getOrderingMode())
                        || "DINE_IN".equals(subOrder.getMultiOrder().getOrderingMode())))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot manually update status for preorders. They are managed via approval/rejection only.");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        if (!isValidStatusTransition(currentStatus, status, subOrder)) {
            if (OrderStatus.PREPARING.equals(status) && subOrder.getRider() == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot start preparing food. Rider must be assigned first. Current status: " + currentStatus);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot change status from " + currentStatus + " to " + status +
                                ". Status can only be updated to PREPARING after rider accepts (ACCEPTED status), and to OUT_FOR_DELIVERY after PREPARING.");
            }
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Update status
        subOrder.setStatus(status);

        // Set preparation start time if status is PREPARING
        if (OrderStatus.PREPARING.equals(status) && subOrder.getPreparationStartTime() == null) {
            subOrder.setPreparationStartTime(java.time.LocalDateTime.now());
        }

        subOrderRepository.save(subOrder);

        // Update MultiOrder status if needed
        MultiOrder multiOrder = subOrder.getMultiOrder();
        if (multiOrder != null && !status.equals(multiOrder.getStatus())) {
            // Update MultiOrder status to match the sub-order status for delivery orders
            if (!"PREORDER".equals(multiOrder.getOrderingMode()) && !"DINE_IN".equals(multiOrder.getOrderingMode())) {
                multiOrder.setStatus(status);
                multiOrderRepository.save(multiOrder);
            }
        }

        // Notify customer and rider via WebSocket
        if (multiOrder != null) {
            // Notify customer
            if (multiOrder.getUser() != null) {
                webSocketPublisher.publishToUser(
                        multiOrder.getUser().getId(),
                        "ORDER_STATUS_UPDATED",
                        Map.of(
                                "orderId", subOrder.getId(),
                                "status", status,
                                "restaurantName", restaurant.getName()));

                // Send email notification to customer
                try {
                    emailService.sendMultiOrderStatusUpdateEmail(multiOrder.getUser(), multiOrder);
                } catch (Exception e) {
                    System.err.println("Failed to send status update email: " + e.getMessage());
                }
            }

            // Notify rider if assigned
            if (subOrder.getRider() != null) {
                webSocketPublisher.publishOrderUpdate(
                        subOrder.getMultiOrder().getId(),
                        subOrder.getId(),
                        status,
                        subOrder.getRider().getId(),
                        subOrder.getRider().getCurrentLatitude() != null
                                ? subOrder.getRider().getCurrentLatitude().doubleValue()
                                : null,
                        subOrder.getRider().getCurrentLongitude() != null
                                ? subOrder.getRider().getCurrentLongitude().doubleValue()
                                : null);
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Order status updated to " + status);
        return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
    }

    @PostMapping("/restaurants/{id}/preorders/{orderId}/approve")
    public String approvePreorder(@PathVariable Long id,
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());

        // Find the MultiOrder by the SubOrder ID
        SubOrder subOrder = subOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!subOrder.getRestaurant().getId().equals(restaurant.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Check if it's a preorder
        if (subOrder.getPreorderSlot() == null && (subOrder.getMultiOrder() == null
                || (!"PREORDER".equals(subOrder.getMultiOrder().getOrderingMode())
                        && !"DINE_IN".equals(subOrder.getMultiOrder().getOrderingMode())))) {
            redirectAttributes.addFlashAttribute("errorMessage", "This is not a preorder");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        MultiOrder multiOrder = subOrder.getMultiOrder();
        if (multiOrder == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "MultiOrder not found");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Check if status is PENDING or PENDING_APPROVAL (allow both for flexibility)
        // Also check SubOrder status as fallback
        boolean isPending = "PENDING_APPROVAL".equals(multiOrder.getStatus()) ||
                "PENDING".equals(multiOrder.getStatus()) ||
                "PENDING_APPROVAL".equals(subOrder.getStatus()) ||
                "PENDING".equals(subOrder.getStatus());

        if (!isPending) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This order is not pending approval. MultiOrder status: " + multiOrder.getStatus() +
                            ", SubOrder status: " + subOrder.getStatus());
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Approve the preorder
        multiOrder.setStatus("CONFIRMED");
        subOrder.setStatus("CONFIRMED");
        multiOrderRepository.save(multiOrder);
        subOrderRepository.save(subOrder);

        // Notify customer via WebSocket
        if (multiOrder.getUser() != null) {
            webSocketPublisher.publishToUser(
                    multiOrder.getUser().getId(),
                    "PREORDER_APPROVED",
                    Map.of(
                            "orderId", multiOrder.getId(),
                            "subOrderId", subOrder.getId(),
                            "restaurantName", restaurant.getName(),
                            "message", "Your preorder has been approved by " + restaurant.getName()));

            // Send email notification to customer
            try {
                emailService.sendPreorderApprovalEmail(multiOrder.getUser(), multiOrder, restaurant.getName());
            } catch (Exception e) {
                System.err.println("Failed to send preorder approval email: " + e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Preorder approved successfully");
        return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
    }

    @PostMapping("/restaurants/{id}/preorders/{orderId}/reject")
    public String rejectPreorder(@PathVariable Long id,
            @PathVariable Long orderId,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());

        // Find the MultiOrder by the SubOrder ID
        SubOrder subOrder = subOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!subOrder.getRestaurant().getId().equals(restaurant.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Check if it's a preorder
        if (subOrder.getPreorderSlot() == null && (subOrder.getMultiOrder() == null
                || (!"PREORDER".equals(subOrder.getMultiOrder().getOrderingMode())
                        && !"DINE_IN".equals(subOrder.getMultiOrder().getOrderingMode())))) {
            redirectAttributes.addFlashAttribute("errorMessage", "This is not a preorder");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        MultiOrder multiOrder = subOrder.getMultiOrder();
        if (multiOrder == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "MultiOrder not found");
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Check if status is PENDING or PENDING_APPROVAL (allow both for flexibility)
        // Also check SubOrder status as fallback
        boolean isPending = "PENDING_APPROVAL".equals(multiOrder.getStatus()) ||
                "PENDING".equals(multiOrder.getStatus()) ||
                "PENDING_APPROVAL".equals(subOrder.getStatus()) ||
                "PENDING".equals(subOrder.getStatus());

        if (!isPending) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This order is not pending approval. MultiOrder status: " + multiOrder.getStatus() +
                            ", SubOrder status: " + subOrder.getStatus());
            return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
        }

        // Release the preorder slot
        if (subOrder.getPreorderSlot() != null) {
            preorderSlotRepository.releaseSlotAtomic(subOrder.getPreorderSlot().getId());
        }

        // Reject the preorder
        multiOrder.setStatus("REJECTED");
        subOrder.setStatus("REJECTED");
        multiOrderRepository.save(multiOrder);
        subOrderRepository.save(subOrder);

        // Notify customer via WebSocket
        if (multiOrder.getUser() != null) {
            webSocketPublisher.publishToUser(
                    multiOrder.getUser().getId(),
                    "PREORDER_REJECTED",
                    Map.of(
                            "orderId", multiOrder.getId(),
                            "subOrderId", subOrder.getId(),
                            "restaurantName", restaurant.getName(),
                            "reason", reason != null ? reason : "No reason provided",
                            "message", "Your preorder has been rejected by " + restaurant.getName() +
                                    (reason != null ? ". Reason: " + reason : "")));

            // Send email notification to customer
            try {
                emailService.sendPreorderRejectionEmail(multiOrder.getUser(), multiOrder, restaurant.getName(), reason);
            } catch (Exception e) {
                System.err.println("Failed to send preorder rejection email: " + e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Preorder rejected successfully");
        return "redirect:/owner/restaurants/" + restaurant.getId() + "/orders";
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus, SubOrder subOrder) {
        // Owner can only update to PREPARING or OUT_FOR_DELIVERY
        if (!OrderStatus.PREPARING.equals(newStatus) && !OrderStatus.OUT_FOR_DELIVERY.equals(newStatus)) {
            return false;
        }

        // Cannot update if already delivered, completed, or cancelled
        if (OrderStatus.DELIVERED.equals(currentStatus) ||
                "COMPLETED".equals(currentStatus) ||
                OrderStatus.CANCELLED.equals(currentStatus)) {
            return false;
        }

        // Can set to PREPARING only from ACCEPTED (after rider accepts)
        // This ensures rider is assigned before owner can start preparing
        if (OrderStatus.PREPARING.equals(newStatus)) {
            // Must have rider assigned and status must be ACCEPTED
            if (subOrder.getRider() == null) {
                return false; // No rider assigned yet
            }
            return "ACCEPTED".equals(currentStatus);
        }

        // Can set to OUT_FOR_DELIVERY only from PREPARING
        if (OrderStatus.OUT_FOR_DELIVERY.equals(newStatus)) {
            return OrderStatus.PREPARING.equals(currentStatus);
        }

        return false;
    }

    @GetMapping("/restaurants/{id}/donations")
    public String viewDonations(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("donations", donationService.getRestaurantDonations(restaurant));
        return "owner/donations";
    }

    @GetMapping("/restaurants/{id}/donations/new")
    public String newDonation(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        if (!model.containsAttribute("donationForm")) {
            model.addAttribute("donationForm", new DonationForm());
        }
        model.addAttribute("restaurant", restaurant);
        return "owner/donation-form";
    }

    @PostMapping("/restaurants/{id}/donations")
    public String createDonation(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("donationForm") DonationForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
            model.addAttribute("restaurant", restaurant);
            return "owner/donation-form";
        }

        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        donationService.createDonation(restaurant, form);
        return "redirect:/owner/restaurants/" + id + "/donations";
    }

    private RestaurantForm toRestaurantForm(Restaurant restaurant) {
        RestaurantForm form = new RestaurantForm();
        form.setName(restaurant.getName());
        form.setDescription(restaurant.getDescription());
        form.setAddress(restaurant.getAddress());
        form.setContactNumber(restaurant.getContactNumber());
        form.setImageUrl(restaurant.getImageUrl());
        form.setOpeningTime(restaurant.getOpeningTime());
        form.setClosingTime(restaurant.getClosingTime());
        form.setBusinessType(restaurant.isCafeLounge() ? "CAFE" : "RESTAURANT");

        // Check if restaurant has tables (dine-in enabled)
        List<RestaurantTable> tables = tableService.getActiveTablesByRestaurant(restaurant.getId());
        if (!tables.isEmpty()) {
            form.setHasDineIn(true);
            form.setNumberOfTables(tables.size());
            // Get most common capacity
            int mostCommonCapacity = tables.stream()
                    .mapToInt(RestaurantTable::getCapacity)
                    .max()
                    .orElse(4);
            form.setDefaultTableCapacity(mostCommonCapacity);
        } else {
            form.setHasDineIn(false);
        }

        return form;
    }

    private MenuForm toMenuForm(Menu menu) {
        MenuForm form = new MenuForm();
        form.setTitle(menu.getTitle());
        form.setDescription(menu.getDescription());
        form.setType(Menu.Type.RESTAURANT.name());
        if (menu.getRestaurant() != null) {
            form.setRestaurantId(menu.getRestaurant().getId());
        }
        return form;
    }

    private MenuItemForm toMenuItemForm(MenuItem item) {
        MenuItemForm form = new MenuItemForm();
        form.setName(item.getName());
        form.setDescription(item.getDescription());
        form.setPrice(item.getPrice());
        form.setAvailable(item.isAvailable());
        form.setTags(item.getTags());
        form.setImageUrl(item.getImageUrl());
        return form;
    }

    private void addMenuItemTagOptions(Model model) {
        model.addAttribute("availableTags", RestaurantTag.allTags());
    }

    private Menu requireOwnedMenu(Long menuId, User owner) {
        Menu menu = menuService.getMenu(menuId);
        if (menu.getRestaurant() == null || !menu.getRestaurant().getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Access denied");
        }
        return menu;
    }

    private MenuItem requireOwnedMenuItem(Long itemId, User owner) {
        // Use getMenuItemForEdit when loading for edit form to preserve imageUrl
        MenuItem item = menuItemService.getMenuItemForEdit(itemId);
        if (item.getMenu().getRestaurant() == null
                || !item.getMenu().getRestaurant().getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Access denied");
        }
        return item;
    }

    // Subscription Package Management
    @GetMapping("/restaurants/{id}/subscription-packages")
    public String viewSubscriptionPackages(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        List<SubscriptionPackage> packages = subscriptionPackageRepository.findByRestaurantAndIsActiveTrue(restaurant);

        // Load menu items for each package into a map
        Map<Long, List<MenuItem>> packageItemsMap = new HashMap<>();
        for (SubscriptionPackage pkg : packages) {
            List<MenuItem> menuItems = subscriptionPackageService.getPackageMenuItems(pkg);
            packageItemsMap.put(pkg.getId(), menuItems);
        }

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("packages", packages);
        model.addAttribute("packageItemsMap", packageItemsMap);
        return "owner/subscription-packages";
    }

    @GetMapping("/restaurants/{id}/subscription-packages/new")
    public String newSubscriptionPackage(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());
        List<Menu> menus = menuService.findByRestaurant(restaurant);
        List<MenuItem> allMenuItems = menus.stream()
                .flatMap(menu -> menuItemService.getItemsForMenu(menu.getId()).stream())
                .filter(MenuItem::isAvailable)
                .collect(java.util.stream.Collectors.toList());

        if (!model.containsAttribute("subscriptionPackageForm")) {
            model.addAttribute("subscriptionPackageForm", new SubscriptionPackageForm());
        }
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", allMenuItems);
        return "owner/subscription-package-form";
    }

    @PostMapping("/restaurants/{id}/subscription-packages")
    public String createSubscriptionPackage(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("subscriptionPackageForm") SubscriptionPackageForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(id, principal.getUser());

        if (bindingResult.hasErrors()) {
            List<Menu> menus = menuService.findByRestaurant(restaurant);
            List<MenuItem> allMenuItems = menus.stream()
                    .flatMap(menu -> menuItemService.getItemsForMenu(menu.getId()).stream())
                    .filter(MenuItem::isAvailable)
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("restaurant", restaurant);
            model.addAttribute("menuItems", allMenuItems);
            return "owner/subscription-package-form";
        }

        subscriptionPackageService.createPackage(restaurant, form);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription package created successfully!");
        return "redirect:/owner/restaurants/" + id + "/subscription-packages";
    }

    @GetMapping("/restaurants/{restaurantId}/subscription-packages/{packageId}/edit")
    public String editSubscriptionPackage(@PathVariable Long restaurantId,
            @PathVariable Long packageId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        SubscriptionPackage pkg = subscriptionPackageService.getPackageById(packageId);

        if (!pkg.getRestaurant().getId().equals(restaurantId)) {
            throw new SecurityException("Access denied");
        }

        List<Menu> menus = menuService.findByRestaurant(restaurant);
        List<MenuItem> allMenuItems = menus.stream()
                .flatMap(menu -> menuItemService.getItemsForMenu(menu.getId()).stream())
                .filter(MenuItem::isAvailable)
                .collect(java.util.stream.Collectors.toList());

        List<MenuItem> selectedMenuItems = subscriptionPackageService.getPackageMenuItems(pkg);
        List<Long> selectedMenuItemIds = selectedMenuItems.stream()
                .map(MenuItem::getId)
                .collect(java.util.stream.Collectors.toList());

        SubscriptionPackageForm form = new SubscriptionPackageForm();
        form.setName(pkg.getName());
        form.setDescription(pkg.getDescription());
        form.setNumberOfPeople(pkg.getNumberOfPeople());
        form.setPrice(pkg.getPrice());
        form.setMealType(pkg.getMealType());
        form.setMenuItemIds(selectedMenuItemIds);

        model.addAttribute("subscriptionPackageForm", form);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", allMenuItems);
        model.addAttribute("packageId", packageId);
        return "owner/subscription-package-form";
    }

    @GetMapping("/restaurants/{restaurantId}/subscription-packages/{packageId}/subscribers")
    public String viewPackageSubscribers(@PathVariable Long restaurantId,
            @PathVariable Long packageId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        SubscriptionPackage pkg = subscriptionPackageService.getPackageById(packageId);

        if (!pkg.getRestaurant().getId().equals(restaurantId)) {
            throw new SecurityException("Access denied");
        }

        List<CompanySubscription> subscribers = companySubscriptionRepository.findBySubscriptionPackage(pkg);
        List<MenuItem> allowedItems = subscriptionPackageService.getPackageMenuItems(pkg);

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("pkg", pkg);
        model.addAttribute("subscribers", subscribers);
        model.addAttribute("allowedItems", allowedItems);
        return "owner/subscription-subscribers";
    }

    @PostMapping("/restaurants/{restaurantId}/subscription-packages/{packageId}/edit")
    public String updateSubscriptionPackage(@PathVariable Long restaurantId,
            @PathVariable Long packageId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("subscriptionPackageForm") SubscriptionPackageForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());

        if (bindingResult.hasErrors()) {
            List<Menu> menus = menuService.findByRestaurant(restaurant);
            List<MenuItem> allMenuItems = menus.stream()
                    .flatMap(menu -> menuItemService.getItemsForMenu(menu.getId()).stream())
                    .filter(MenuItem::isAvailable)
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("restaurant", restaurant);
            model.addAttribute("menuItems", allMenuItems);
            model.addAttribute("packageId", packageId);
            return "owner/subscription-package-form";
        }

        SubscriptionPackage existingPackage = subscriptionPackageService.getPackageById(packageId);
        if (!companySubscriptionRepository.findBySubscriptionPackage(existingPackage).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Package already has subscribers or pending requests. Items are fixed and cannot be changed.");
            return "redirect:/owner/restaurants/" + restaurantId + "/subscription-packages";
        }

        subscriptionPackageService.updatePackage(packageId, restaurant, form);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription package updated successfully!");
        return "redirect:/owner/restaurants/" + restaurantId + "/subscription-packages";
    }

    @PostMapping("/restaurants/{restaurantId}/subscription-packages/{packageId}/delete")
    public String deleteSubscriptionPackage(@PathVariable Long restaurantId,
            @PathVariable Long packageId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        subscriptionPackageService.deletePackage(packageId, restaurant);
        redirectAttributes.addFlashAttribute("successMessage", "Package deactivated successfully!");
        return "redirect:/owner/restaurants/" + restaurantId + "/subscription-packages";
    }

    @GetMapping("/restaurants/{restaurantId}/subscription-requests")
    public String viewSubscriptionRequests(@PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        List<CompanySubscription> pendingRequests = companyService.getPendingRequestsForRestaurant(restaurant);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("pendingRequests", pendingRequests);
        return "owner/subscription-requests";
    }

    @PostMapping("/restaurants/{restaurantId}/subscription-requests/{subscriptionId}/approve")
    public String approveSubscriptionRequest(@PathVariable Long restaurantId,
            @PathVariable Long subscriptionId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        try {
            companyService.approveSubscriptionRequest(subscriptionId, restaurant, note);
            redirectAttributes.addFlashAttribute("successMessage", "Request approved. Company can now pay.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/owner/restaurants/" + restaurantId + "/subscription-requests";
    }

    @PostMapping("/restaurants/{restaurantId}/subscription-requests/{subscriptionId}/reject")
    public String rejectSubscriptionRequest(@PathVariable Long restaurantId,
            @PathVariable Long subscriptionId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        try {
            companyService.rejectSubscriptionRequest(subscriptionId, restaurant, note);
            redirectAttributes.addFlashAttribute("successMessage", "Request rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/owner/restaurants/" + restaurantId + "/subscription-requests";
    }

    // --- Offers / Coupons Management ---

    @GetMapping("/restaurants/{restaurantId}/offers")
    public String viewOffers(@PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        List<Offer> offers = offerService.getAllOffersByRestaurant(restaurant);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("offers", offers);
        return "owner/offers";
    }

    @GetMapping("/restaurants/{restaurantId}/offers/new")
    public String newOffer(@PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        Offer offer = new Offer();
        offer.setRestaurant(restaurant);
        offer.setOfferType("PERCENTAGE_OFF"); // default

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("offer", offer);
        return "owner/offer-form";
    }

    @PostMapping("/restaurants/{restaurantId}/offers")
    public String createOffer(@PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("offer") Offer offer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());

        if (bindingResult.hasErrors()) {
            model.addAttribute("restaurant", restaurant);
            return "owner/offer-form";
        }

        offer.setRestaurant(restaurant);
        offerService.createOffer(offer);

        redirectAttributes.addFlashAttribute("successMessage", "Offer created successfully!");
        return "redirect:/owner/restaurants/" + restaurantId + "/offers";
    }

    @GetMapping("/restaurants/{restaurantId}/offers/{offerId}/edit")
    public String editOffer(@PathVariable Long restaurantId,
            @PathVariable Long offerId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        Offer offer = offerService.getAllOffersByRestaurant(restaurant).stream()
                .filter(o -> o.getId().equals(offerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("offer", offer);
        return "owner/offer-form";
    }

    @PostMapping("/restaurants/{restaurantId}/offers/{offerId}/edit")
    public String updateOffer(@PathVariable Long restaurantId,
            @PathVariable Long offerId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute("offer") Offer offer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());

        if (bindingResult.hasErrors()) {
            model.addAttribute("restaurant", restaurant);
            return "owner/offer-form";
        }

        offerService.updateOffer(offerId, offer);

        redirectAttributes.addFlashAttribute("successMessage", "Offer updated successfully!");
        return "redirect:/owner/restaurants/" + restaurantId + "/offers";
    }

    @PostMapping("/restaurants/{restaurantId}/offers/{offerId}/delete")
    public String deleteOffer(@PathVariable Long restaurantId,
            @PathVariable Long offerId,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        // verify ownership
        Restaurant restaurant = restaurantService.getOwnedRestaurant(restaurantId, principal.getUser());
        // ideally verify offer belongs to restaurant too
        offerService.deleteOffer(offerId);

        redirectAttributes.addFlashAttribute("successMessage", "Offer deleted successfully!");
        return "redirect:/owner/restaurants/" + restaurantId + "/offers";
    }
}
