package com.srFoodDelivery.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srFoodDelivery.model.Company;
import com.srFoodDelivery.model.CompanyOrder;
import com.srFoodDelivery.model.CompanySubscription;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubscriptionItem;
import com.srFoodDelivery.model.SubscriptionPackage;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.UserRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.CompanyService;
import com.srFoodDelivery.service.SubscriptionPackageService;
import com.srFoodDelivery.repository.SubscriptionItemRepository;

@Controller
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final SubscriptionPackageService subscriptionPackageService;
    private final UserRepository userRepository;

    public CompanyController(
            CompanyService companyService,
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            SubscriptionPackageService subscriptionPackageService,
            UserRepository userRepository) {
        this.companyService = companyService;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.subscriptionPackageService = subscriptionPackageService;
        this.userRepository = userRepository;
    }

    private User requireUser(CustomUserDetails principal) {
        if (principal == null) {
            throw new SecurityException("Not authenticated");
        }
        return principal.getUser();
    }

    @GetMapping({ "", "/dashboard" })
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        Optional<CompanySubscription> activeSubscription = companyService.getActiveSubscription(company);
        List<CompanyOrder> recentOrders = companyService.getCompanyOrders(company);

        model.addAttribute("company", company);
        model.addAttribute("user", user);
        model.addAttribute("activeSubscription", activeSubscription.orElse(null));
        model.addAttribute("recentOrders", recentOrders.size() > 10 ? recentOrders.subList(0, 10) : recentOrders);
        model.addAttribute("totalOrders", recentOrders.size());

        return "company/dashboard";
    }

    @GetMapping("/profile")
    public String showProfileForm(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);
        model.addAttribute("company", company);
        return "company/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String companyName,
            @RequestParam(required = false) String companyAddress,
            @RequestParam(required = false) String officePhone,
            @RequestParam(required = false) String contactPersonName,
            @RequestParam(required = false) Integer numberOfEmployees,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        companyService.updateCompanyProfile(company, companyName, companyAddress,
                officePhone, contactPersonName, numberOfEmployees);

        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/company/profile";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        // Get all restaurants with their packages
        List<Restaurant> restaurants = restaurantRepository.findByIsActiveTrue();
        Optional<CompanySubscription> activeSubscription = companyService.getActiveSubscription(company);
        List<CompanySubscription> subscriptionHistory = companyService.getCompanySubscriptions(company);
        List<CompanySubscription> companyRequests = companyService.getCompanyRequests(company);

        // Load subscription items for active subscription if exists
        if (activeSubscription.isPresent()) {
            List<SubscriptionItem> items = subscriptionItemRepository.findBySubscription(activeSubscription.get());
            activeSubscription.get().setItems(items);
        }

        model.addAttribute("company", company);
        model.addAttribute("restaurants", restaurants);
        model.addAttribute("activeSubscription", activeSubscription.orElse(null));
        model.addAttribute("subscriptionHistory", subscriptionHistory);
        model.addAttribute("companyRequests", companyRequests);

        return "company/subscriptions";
    }

    @GetMapping("/subscriptions/{subscriptionId}/edit-items")
    public String editSubscriptionItems(@PathVariable Long subscriptionId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        CompanySubscription subscription = companyService.getCompanySubscription(subscriptionId);
        if (!subscription.getCompany().getId().equals(company.getId())) {
            throw new SecurityException("Access denied");
        }

        SubscriptionPackage subscriptionPackage = subscription.getSubscriptionPackage();
        List<MenuItem> allowedItems = subscriptionPackageService.getPackageMenuItems(subscriptionPackage);
        List<SubscriptionItem> selectedItems = subscriptionItemRepository.findBySubscription(subscription);
        java.util.Set<Long> selectedIds = selectedItems.stream()
                .map(item -> item.getMenuItem().getId())
                .collect(java.util.stream.Collectors.toSet());

        model.addAttribute("subscription", subscription);
        model.addAttribute("package", subscriptionPackage);
        model.addAttribute("restaurant", subscriptionPackage.getRestaurant());
        model.addAttribute("allowedItems", allowedItems);
        model.addAttribute("selectedIds", selectedIds);
        return "company/edit-subscription-items";
    }

    @PostMapping("/subscriptions/{subscriptionId}/edit-items")
    public String updateSubscriptionItems(@PathVariable Long subscriptionId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "menuItemIds", required = false) List<Long> menuItemIds,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);
        try {
            companyService.updateSubscriptionItems(company, subscriptionId, menuItemIds);
            redirectAttributes.addFlashAttribute("success", "Subscription items updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/company/subscriptions/" + subscriptionId + "/edit-items";
        }
        return "redirect:/company/subscriptions";
    }

    @GetMapping("/subscriptions/restaurant/{restaurantId}")
    public String restaurantPackages(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        List<SubscriptionPackage> packages = companyService.getPackagesByRestaurant(restaurant);
        java.util.Map<Long, List<MenuItem>> packageItemsMap = new java.util.HashMap<>();
        for (SubscriptionPackage pkg : packages) {
            List<MenuItem> allowedItems = subscriptionPackageService.getPackageMenuItems(pkg);
            packageItemsMap.put(pkg.getId(), allowedItems);
        }

        model.addAttribute("company", company);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("packages", packages);
        model.addAttribute("packageItemsMap", packageItemsMap);

        return "company/restaurant-packages";
    }

    @GetMapping("/subscriptions/select-items/{packageId}")
    public String selectSubscriptionItems(
            @PathVariable Long packageId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        SubscriptionPackage subscriptionPackage = companyService.getPackageById(packageId);
        Restaurant restaurant = subscriptionPackage.getRestaurant();

        // Only show menu items allowed for this package
        List<MenuItem> menuItems = subscriptionPackageService.getPackageMenuItems(subscriptionPackage);

        model.addAttribute("company", company);
        model.addAttribute("package", subscriptionPackage);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", menuItems);

        return "company/select-subscription-items";
    }

    @PostMapping("/subscriptions/subscribe")
    public String subscribe(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long packageId,
            @RequestParam Integer peopleCount,
            @RequestParam(name = "duration", defaultValue = "MONTH") String duration,
            @RequestParam(name = "preferredTime", required = false) String preferredTime,
            @RequestParam(name = "excludedDays", required = false) String excludedDays,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        try {
            int durationMonths = "YEAR".equalsIgnoreCase(duration) ? 12 : 1;
            companyService.requestSubscription(company, packageId, durationMonths, preferredTime,
                    excludedDays);
            redirectAttributes.addFlashAttribute("success",
                    "Subscription prepared! Please complete payment to activate.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to request: " + e.getMessage());
            return "redirect:/company/subscriptions/restaurant/"
                    + companyService.getPackageById(packageId).getRestaurant().getId();
        }

        return "redirect:/company/subscriptions";
    }

    @GetMapping("/subscriptions/{subscriptionId}/payment")
    public String showPaymentPage(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);
        CompanySubscription subscription = companyService.getCompanySubscription(subscriptionId);

        if (!subscription.getCompany().getId().equals(company.getId())) {
            throw new SecurityException("Access denied");
        }

        model.addAttribute("subscription", subscription);
        return "company/payment";
    }

    @PostMapping("/subscriptions/{subscriptionId}/pay")
    public String payForSubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "DEMO") String paymentMethod,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String deliveryAddress,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);
        try {
            String txId = (transactionId != null && !transactionId.isBlank())
                    ? transactionId.trim()
                    : "SUB-" + System.currentTimeMillis();
            CompanySubscription subscription = companyService.getCompanySubscription(subscriptionId);
            if (!subscription.getCompany().getId().equals(company.getId())) {
                throw new SecurityException("Access denied");
            }
            companyService.markSubscriptionPaid(subscriptionId, paymentMethod, txId, deliveryAddress);
            redirectAttributes.addFlashAttribute("success", "Payment successful and subscription activated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment failed: " + e.getMessage());
        }
        return "redirect:/company/subscriptions";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        List<CompanyOrder> orders = companyService.getCompanyOrders(company);
        Optional<CompanySubscription> activeSubscription = companyService.getActiveSubscription(company);

        model.addAttribute("company", company);
        model.addAttribute("orders", orders);
        model.addAttribute("activeSubscription", activeSubscription.orElse(null));

        return "company/orders";
    }

    @GetMapping("/orders/new")
    public String newOrder(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        Optional<CompanySubscription> activeSubscription = companyService.getActiveSubscription(company);
        if (activeSubscription.isEmpty()) {
            model.addAttribute("error", "You need an active subscription to place orders");
            return "redirect:/company/subscriptions";
        }

        CompanySubscription subscription = activeSubscription.get();
        Restaurant restaurant = subscription.getSubscriptionPackage().getRestaurant();
        List<SubscriptionItem> subscriptionItems = subscriptionItemRepository.findBySubscription(subscription);
        List<MenuItem> subscriptionMenuItems = subscriptionItems.stream()
                .map(SubscriptionItem::getMenuItem)
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("company", company);
        model.addAttribute("activeSubscription", subscription);
        model.addAttribute("menuItems", subscriptionMenuItems);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("orderDate", LocalDate.now());

        return "company/new-order";
    }

    @PostMapping("/orders/place")
    public String placeOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam LocalDate orderDate,
            @RequestParam String deliveryAddress,
            @RequestParam(required = false) String specialInstructions,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        try {
            companyService.placeDailyOrder(company, orderDate, deliveryAddress, specialInstructions);
            redirectAttributes.addFlashAttribute("success",
                    "Daily order placed successfully for all subscription items!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to place order: " + e.getMessage());
            return "redirect:/company/orders/new";
        }

        return "redirect:/company/orders";
    }

    @GetMapping("/restaurants")
    public String restaurants(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        model.addAttribute("restaurants", restaurants);
        return "company/restaurants";
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public String restaurantMenu(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {
        User user = requireUser(principal);
        Company company = companyService.getOrCreateCompany(user);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        Optional<CompanySubscription> activeSubscription = companyService.getActiveSubscription(company);
        if (activeSubscription.isEmpty()) {
            model.addAttribute("error", "You need an active subscription to view menus");
            return "redirect:/company/subscriptions";
        }

        CompanySubscription subscription = activeSubscription.get();

        // Get subscription items
        List<SubscriptionItem> subscriptionItems = subscriptionItemRepository.findBySubscription(subscription);
        List<Long> subscriptionItemIds = subscriptionItems.stream()
                .map(item -> item.getMenuItem().getId())
                .collect(java.util.stream.Collectors.toList());

        // Filter menu items to only show those in the subscription
        List<MenuItem> allMenuItems = menuItemRepository.findByMenu_Restaurant_IdAndAvailableTrue(restaurantId);
        List<MenuItem> menuItems = allMenuItems.stream()
                .filter(item -> subscriptionItemIds.contains(item.getId()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("company", company);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("activeSubscription", subscription);
        model.addAttribute("orderDate", LocalDate.now());

        return "company/restaurant-menu";
    }

    @GetMapping("/delete-my-account")
    @SuppressWarnings("null")
    public String deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);

        // Find company (don't create if not exists, though getOrCreate handles it)
        Optional<Company> companyOpt = companyService.findByUser(user);

        if (companyOpt.isPresent()) {
            companyService.deleteAllCompanyData(companyOpt.get());
        }

        userRepository.delete(user);

        return "redirect:/logout";
    }
}
