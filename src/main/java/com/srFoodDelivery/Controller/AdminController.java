package com.srFoodDelivery.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.OrderStatus;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.model.UserRole;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.UserRepository;
import com.srFoodDelivery.service.DonationService;
import com.srFoodDelivery.service.MenuItemService;
import com.srFoodDelivery.service.OrderService;
import com.srFoodDelivery.service.RestaurantService;
import com.srFoodDelivery.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderService orderService;
    private final DonationService donationService;
    private final MenuItemService menuItemService;
    private final RestaurantService restaurantService;
    private final UserService userService;

    public AdminController(UserRepository userRepository,
                          RestaurantRepository restaurantRepository,
                          OrderService orderService,
                          DonationService donationService,
                          MenuItemService menuItemService,
                          RestaurantService restaurantService,
                          UserService userService) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderService = orderService;
        this.donationService = donationService;
        this.menuItemService = menuItemService;
        this.restaurantService = restaurantService;
        this.userService = userService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        long totalUsers = userRepository.count();
        long totalRestaurants = restaurantRepository.count();
        long totalOrders = orderService.getAllOrders().size();
        long totalDonations = donationService.getAvailableDonations().size();
        
        // Role-based statistics
        long totalCustomers = userService.countByRole(UserRole.CUSTOMER);
        long totalOwners = userService.countByRole(UserRole.OWNER);
        long totalCafeOwners = userService.countByRole(UserRole.CAFE_OWNER);
        long totalRiders = userService.countByRole(UserRole.RIDER);
        long totalAdmins = userService.countByRole(UserRole.ADMIN);
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalRestaurants", totalRestaurants);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalDonations", totalDonations);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOwners", totalOwners);
        model.addAttribute("totalCafeOwners", totalCafeOwners);
        model.addAttribute("totalRiders", totalRiders);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("recentOrders", orderService.getAllOrders().stream().limit(10).toList());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model, @RequestParam(required = false) String role) {
        if (role != null && !role.isEmpty()) {
            model.addAttribute("users", userService.findByRole(role));
            model.addAttribute("selectedRole", role);
        } else {
            model.addAttribute("users", userRepository.findAll());
        }
        model.addAttribute("roles", new String[]{
            UserRole.CUSTOMER, UserRole.OWNER, UserRole.CAFE_OWNER, UserRole.RIDER, UserRole.ADMIN
        });
        return "admin/users";
    }

    @GetMapping("/users/{userId}")
    public String viewUser(@PathVariable Long userId, Model model) {
        User user = userService.getById(userId);
        model.addAttribute("user", user);
        model.addAttribute("restaurants", restaurantRepository.findByOwner(user));
        return "admin/user-detail";
    }

    @GetMapping("/users/{userId}/edit")
    public String editUserForm(@PathVariable Long userId, Model model) {
        User user = userService.getById(userId);
        model.addAttribute("user", user);
        model.addAttribute("roles", new String[]{
            UserRole.CUSTOMER, UserRole.OWNER, UserRole.CAFE_OWNER, UserRole.RIDER, UserRole.ADMIN
        });
        return "admin/user-edit";
    }

    @PostMapping("/users/{userId}/edit")
    public String updateUser(@PathVariable Long userId,
                            @RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String phoneNumber,
                            @RequestParam String role,
                            @RequestParam(required = false) String deliveryLocation,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(userId, fullName, email, phoneNumber, role, deliveryLocation);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/users/" + userId;
    }

    @GetMapping("/users/{userId}/change-password")
    public String changePasswordForm(@PathVariable Long userId, Model model) {
        User user = userService.getById(userId);
        model.addAttribute("user", user);
        return "admin/user-change-password";
    }

    @PostMapping("/users/{userId}/change-password")
    public String changePassword(@PathVariable Long userId,
                                @RequestParam String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            if (newPassword == null || newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters");
                return "redirect:/admin/users/" + userId + "/change-password";
            }
            userService.updateUserPassword(userId, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating password: " + e.getMessage());
        }
        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/restaurants")
    public String listRestaurants(Model model, 
                                 @RequestParam(required = false) Long ownerId,
                                 @RequestParam(required = false) String status) {
        if (ownerId != null) {
            User owner = userService.getById(ownerId);
            model.addAttribute("restaurants", restaurantRepository.findByOwner(owner));
            model.addAttribute("selectedOwner", owner);
        } else if (status != null && status.equals("active")) {
            model.addAttribute("restaurants", restaurantRepository.findByIsActiveTrue());
            model.addAttribute("selectedStatus", "active");
        } else if (status != null && status.equals("inactive")) {
            model.addAttribute("restaurants", restaurantRepository.findAll().stream()
                .filter(r -> !r.isActive()).toList());
            model.addAttribute("selectedStatus", "inactive");
        } else {
            model.addAttribute("restaurants", restaurantRepository.findAll());
        }
        
        // Get all owners and cafe owners for filter
        model.addAttribute("owners", userService.findByRole(UserRole.OWNER));
        model.addAttribute("cafeOwners", userService.findByRole(UserRole.CAFE_OWNER));
        return "admin/restaurants";
    }

    @GetMapping("/restaurants/{restaurantId}")
    public String viewRestaurant(@PathVariable Long restaurantId, Model model) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        model.addAttribute("restaurant", restaurant);
        return "admin/restaurant-detail";
    }

    @GetMapping("/restaurants/{restaurantId}/edit")
    public String editRestaurantForm(@PathVariable Long restaurantId, Model model) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("owners", userService.findByRole(UserRole.OWNER));
        model.addAttribute("cafeOwners", userService.findByRole(UserRole.CAFE_OWNER));
        return "admin/restaurant-edit";
    }

    @PostMapping("/restaurants/{restaurantId}/edit")
    public String updateRestaurant(@PathVariable Long restaurantId,
                                   @RequestParam String name,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String address,
                                   @RequestParam(required = false) String contactNumber,
                                   @RequestParam Long ownerId,
                                   @RequestParam(required = false) String imageUrl,
                                   @RequestParam(required = false) String openingTime,
                                   @RequestParam(required = false) String closingTime,
                                   @RequestParam(required = false) String cuisineType,
                                   @RequestParam(required = false, defaultValue = "false") boolean isPureVeg,
                                   @RequestParam(required = false, defaultValue = "false") boolean isCloudKitchen,
                                   @RequestParam(required = false, defaultValue = "false") boolean isFamilyRestaurant,
                                   @RequestParam(required = false, defaultValue = "false") boolean isCafeLounge,
                                   @RequestParam(required = false, defaultValue = "true") boolean isActive,
                                   RedirectAttributes redirectAttributes) {
        try {
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            User owner = userService.getById(ownerId);
            
            restaurant.setName(name);
            restaurant.setDescription(description);
            restaurant.setAddress(address);
            restaurant.setContactNumber(contactNumber);
            restaurant.setOwner(owner);
            restaurant.setImageUrl(imageUrl);
            restaurant.setOpeningTime(openingTime);
            restaurant.setClosingTime(closingTime);
            restaurant.setCuisineType(cuisineType);
            restaurant.setPureVeg(isPureVeg);
            restaurant.setCloudKitchen(isCloudKitchen);
            restaurant.setFamilyRestaurant(isFamilyRestaurant);
            restaurant.setCafeLounge(isCafeLounge);
            restaurant.setActive(isActive);
            
            restaurantRepository.save(restaurant);
            redirectAttributes.addFlashAttribute("successMessage", "Restaurant updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating restaurant: " + e.getMessage());
        }
        return "redirect:/admin/restaurants/" + restaurantId;
    }

    @PostMapping("/restaurants/{restaurantId}/toggle-active")
    public String toggleRestaurantActive(@PathVariable Long restaurantId, RedirectAttributes redirectAttributes) {
        try {
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            restaurant.setActive(!restaurant.isActive());
            restaurantRepository.save(restaurant);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Restaurant " + (restaurant.isActive() ? "activated" : "deactivated") + " successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating restaurant: " + e.getMessage());
        }
        return "redirect:/admin/restaurants/" + restaurantId;
    }

    @PostMapping("/restaurants/{restaurantId}/delete")
    public String deleteRestaurant(@PathVariable Long restaurantId, RedirectAttributes redirectAttributes) {
        try {
            restaurantRepository.deleteById(restaurantId);
            redirectAttributes.addFlashAttribute("successMessage", "Restaurant deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting restaurant: " + e.getMessage());
        }
        return "redirect:/admin/restaurants";
    }


    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String viewOrder(@PathVariable Long orderId, Model model) {
        Order order = orderService.getOrder(orderId);
        model.addAttribute("order", order);
        model.addAttribute("statuses", new String[]{
            OrderStatus.NEW,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        });
        return "admin/order-detail";
    }

    @PostMapping("/orders/{orderId}/status")
    public String updateOrderStatus(@PathVariable Long orderId,
                                   @RequestParam("status") String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating order status: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + orderId;
    }

    @GetMapping("/donations")
    public String listDonations(Model model) {
        model.addAttribute("donations", donationService.getAvailableDonations());
        return "admin/donations";
    }

    @GetMapping("/reviews")
    public String listReviews(Model model) {
        model.addAttribute("restaurants", restaurantRepository.findAll());
        return "admin/reviews";
    }

    @PostMapping("/cafe-items/delete-all")
    public String deleteAllCafeItems(RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = menuItemService.deleteAllCafeItems();
            redirectAttributes.addFlashAttribute("successMessage", 
                "Successfully deleted " + deletedCount + " cafe items");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error deleting cafe items: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/restaurants/delete-by-name")
    public String deleteRestaurantByName(@RequestParam("name") String name, 
                                        RedirectAttributes redirectAttributes) {
        try {
            restaurantService.deleteRestaurantByName(name);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Successfully deleted restaurant '" + name + "' and all its items");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error deleting restaurant: " + e.getMessage());
        }
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/restaurants/delete-caffeine-house")
    public String deleteCaffeineHouse(RedirectAttributes redirectAttributes) {
        try {
            restaurantService.deleteRestaurantByName("Caffeine House");
            redirectAttributes.addFlashAttribute("successMessage", 
                "Successfully deleted 'Caffeine House' cafe and all its items");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error deleting Caffeine House: " + e.getMessage());
        }
        return "redirect:/admin/restaurants";
    }
}

