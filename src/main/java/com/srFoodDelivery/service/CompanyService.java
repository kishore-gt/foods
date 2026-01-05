package com.srFoodDelivery.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.Company;
import com.srFoodDelivery.model.CompanyOrder;
import com.srFoodDelivery.model.CompanySubscription;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubscriptionPackage;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.model.PackageMenuItem;
import com.srFoodDelivery.repository.CompanyOrderRepository;
import com.srFoodDelivery.repository.CompanyRepository;
import com.srFoodDelivery.repository.CompanySubscriptionRepository;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.SubscriptionItemRepository;
import com.srFoodDelivery.repository.SubscriptionPackageRepository;
import com.srFoodDelivery.model.SubscriptionItem;
import com.srFoodDelivery.repository.PackageMenuItemRepository;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final CompanyOrderRepository companyOrderRepository;
    private final MenuItemRepository menuItemRepository;
    private final SubscriptionItemRepository subscriptionItemRepository;
    private final PackageMenuItemRepository packageMenuItemRepository;

    public CompanyService(
            CompanyRepository companyRepository,
            SubscriptionPackageRepository subscriptionPackageRepository,
            CompanySubscriptionRepository companySubscriptionRepository,
            CompanyOrderRepository companyOrderRepository,
            MenuItemRepository menuItemRepository,
            SubscriptionItemRepository subscriptionItemRepository,
            PackageMenuItemRepository packageMenuItemRepository) {
        this.companyRepository = companyRepository;
        this.subscriptionPackageRepository = subscriptionPackageRepository;
        this.companySubscriptionRepository = companySubscriptionRepository;
        this.companyOrderRepository = companyOrderRepository;
        this.menuItemRepository = menuItemRepository;
        this.subscriptionItemRepository = subscriptionItemRepository;
        this.packageMenuItemRepository = packageMenuItemRepository;
    }

    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public Company getOrCreateCompany(User user) {
        Optional<Company> existing = companyRepository.findByUser(user);
        if (existing.isPresent()) {
            return existing.get();
        }

        Company company = new Company();
        company.setUser(user);
        company.setCompanyName(user.getFullName() + " Company");
        return companyRepository.save(company);
    }

    public Optional<Company> findByUser(User user) {
        return companyRepository.findByUser(user);
    }

    public Company getByUser(User user) {
        return findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Company profile not found"));
    }

    @Transactional
    public Company updateCompanyProfile(Company company, String companyName, String companyAddress,
            String officePhone, String contactPersonName, Integer numberOfEmployees) {
        company.setCompanyName(companyName);
        company.setCompanyAddress(companyAddress);
        company.setOfficePhone(officePhone);
        company.setContactPersonName(contactPersonName);
        company.setNumberOfEmployees(numberOfEmployees);
        return companyRepository.save(company);
    }

    public List<SubscriptionPackage> getActivePackages() {
        return subscriptionPackageRepository.findByIsActiveTrue();
    }

    public List<SubscriptionPackage> getPackagesByRestaurant(Restaurant restaurant) {
        return subscriptionPackageRepository.findByRestaurantAndIsActiveTrue(restaurant);
    }

    public List<SubscriptionPackage> getPackagesByRestaurantId(Long restaurantId) {
        return subscriptionPackageRepository.findByRestaurantIdAndIsActiveTrue(restaurantId);
    }

    public List<SubscriptionPackage> getPackagesByRestaurantAndNumberOfPeople(Restaurant restaurant,
            Integer numberOfPeople) {
        return subscriptionPackageRepository.findByRestaurantAndNumberOfPeopleAndIsActiveTrue(restaurant,
                numberOfPeople);
    }

    public SubscriptionPackage getPackageById(Long packageId) {
        return subscriptionPackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription package not found"));
    }

    @Transactional
    public CompanySubscription requestSubscription(Company company, Long packageId,
            int durationMonths, String preferredTime, String excludedDays) {
        SubscriptionPackage subscriptionPackage = getPackageById(packageId);

        if (durationMonths != 1 && durationMonths != 12) {
            throw new IllegalArgumentException("Duration must be 1 month or 12 months.");
        }

        List<PackageMenuItem> allowedPackageItems = packageMenuItemRepository
                .findBySubscriptionPackage(subscriptionPackage);
        if (allowedPackageItems.isEmpty()) {
            throw new IllegalArgumentException("This package has no items configured yet.");
        }

        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompany(company);
        subscription.setSubscriptionPackage(subscriptionPackage);
        // Auto-approve the subscription so they can pay immediately
        subscription.setStatus("APPROVED");
        subscription.setPaymentStatus("PENDING");
        // Use the package's defined people count
        subscription.setPeopleCount(subscriptionPackage.getNumberOfPeople());
        subscription.setDurationMonths(durationMonths);
        subscription.setPreferredTime(preferredTime);
        subscription.setExcludedDays(excludedDays);
        subscription.setTotalAmount(calculateTotal(subscriptionPackage, durationMonths));
        subscription.setPaymentAmount(subscription.getTotalAmount());

        return companySubscriptionRepository.save(subscription);
    }

    @Transactional
    public CompanySubscription approveSubscriptionRequest(Long subscriptionId, Restaurant restaurant, String note) {
        CompanySubscription subscription = companySubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription request not found"));

        if (!subscription.getSubscriptionPackage().getRestaurant().getId().equals(restaurant.getId())) {
            throw new SecurityException("Access denied");
        }
        // Auto-approved subscriptions skip this, but keeping for backward compatibility
        // or future manual flows
        subscription.setStatus("APPROVED");
        subscription.setApprovalNote(note);
        return companySubscriptionRepository.save(subscription);
    }

    @Transactional
    public CompanySubscription rejectSubscriptionRequest(Long subscriptionId, Restaurant restaurant, String note) {
        CompanySubscription subscription = companySubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription request not found"));

        if (!subscription.getSubscriptionPackage().getRestaurant().getId().equals(restaurant.getId())) {
            throw new SecurityException("Access denied");
        }

        subscription.setStatus("REJECTED");
        subscription.setApprovalNote(note);
        return companySubscriptionRepository.save(subscription);
    }

    @Transactional
    public CompanySubscription markSubscriptionPaid(Long subscriptionId, String paymentMethod,
            String transactionId, String deliveryAddress) {
        CompanySubscription subscription = companySubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        if (!"APPROVED".equals(subscription.getStatus())) {
            throw new IllegalStateException("Subscription must be approved before payment.");
        }

        int months = subscription.getDurationMonths() != null ? subscription.getDurationMonths() : 1;
        // peopleCount is already set in the subscription

        subscription.setPaymentStatus("PAID");
        subscription.setPaymentMethod(paymentMethod);
        subscription.setTransactionId(transactionId);
        subscription.setStatus("ACTIVE");
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(months));
        // Recalculate total just to be safe, though it should be set on request
        subscription.setTotalAmount(calculateTotal(subscription.getSubscriptionPackage(), months));
        subscription.setPaymentAmount(subscription.getTotalAmount());

        subscriptionItemRepository.deleteBySubscription(subscription);
        List<PackageMenuItem> allowedPackageItems = packageMenuItemRepository
                .findBySubscriptionPackage(subscription.getSubscriptionPackage());

        for (PackageMenuItem pkgItem : allowedPackageItems) {
            SubscriptionItem item = new SubscriptionItem(subscription, pkgItem.getMenuItem());
            subscriptionItemRepository.save(item);
        }

        CompanySubscription saved = companySubscriptionRepository.save(subscription);

        createSubscriptionOrders(saved, deliveryAddress);
        return saved;
    }

    private BigDecimal calculateTotal(SubscriptionPackage subscriptionPackage, int months) {
        // Price is "Daily Group Price" (already includes people count)
        // Monthly Cost = Daily Price * 30 days * Months
        return subscriptionPackage.getPrice()
                .multiply(BigDecimal.valueOf(30))
                .multiply(BigDecimal.valueOf(months));
    }

    @Transactional
    protected void createSubscriptionOrders(CompanySubscription subscription, String deliveryAddress) {
        List<SubscriptionItem> items = subscriptionItemRepository.findBySubscription(subscription);
        if (items.isEmpty()) {
            return;
        }
        int quantity = subscription.getPeopleCount() != null ? subscription.getPeopleCount() : 1;
        String address = (deliveryAddress != null && !deliveryAddress.isBlank())
                ? deliveryAddress
                : "Subscription delivery for company " + subscription.getCompany().getCompanyName();

        for (SubscriptionItem subItem : items) {
            MenuItem menuItem = subItem.getMenuItem();
            BigDecimal totalAmount = menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));

            CompanyOrder order = new CompanyOrder();
            order.setCompany(subscription.getCompany());
            order.setMenuItem(menuItem);
            order.setOrderDate(LocalDate.now());
            order.setQuantity(quantity);
            order.setTotalAmount(totalAmount);
            order.setDeliveryAddress(address);
            order.setSpecialInstructions("Subscription package order");
            order.setStatus("PAID");
            companyOrderRepository.save(order);
        }
    }

    @Transactional
    public void updateSubscriptionItems(Company company, Long subscriptionId, List<Long> menuItemIds) {
        CompanySubscription subscription = companySubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        if (!subscription.getCompany().getId().equals(company.getId())) {
            throw new SecurityException("Access denied");
        }

        if (!"ACTIVE".equals(subscription.getStatus())) {
            throw new IllegalStateException("Items are fixed for this package and cannot be changed.");
        }

        SubscriptionPackage subscriptionPackage = subscription.getSubscriptionPackage();
        List<PackageMenuItem> allowedPackageItems = packageMenuItemRepository
                .findBySubscriptionPackage(subscriptionPackage);
        if (allowedPackageItems.isEmpty()) {
            throw new IllegalArgumentException("No items configured for this package. Contact restaurant owner.");
        }

        if (menuItemIds == null || menuItemIds.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one item for your subscription.");
        }

        java.util.Set<Long> allowedIds = allowedPackageItems.stream()
                .map(item -> item.getMenuItem().getId())
                .collect(java.util.stream.Collectors.toSet());
        boolean allAllowed = menuItemIds.stream().allMatch(allowedIds::contains);
        if (!allAllowed) {
            throw new IllegalArgumentException("Selected items must be within the package's allowed items.");
        }

        // replace subscription items
        subscriptionItemRepository.deleteBySubscription(subscription);
        for (Long menuItemId : menuItemIds) {
            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));
            SubscriptionItem item = new SubscriptionItem(subscription, menuItem);
            subscriptionItemRepository.save(item);
        }
    }

    public Optional<CompanySubscription> getActiveSubscription(Company company) {
        List<CompanySubscription> subscriptions = companySubscriptionRepository
                .findByCompanyAndStatusOrderByStartDateDesc(company, "ACTIVE");
        return subscriptions.isEmpty() ? Optional.empty() : Optional.of(subscriptions.get(0));
    }

    public List<CompanySubscription> getPendingRequestsForRestaurant(Restaurant restaurant) {
        return companySubscriptionRepository
                .findBySubscriptionPackage_Restaurant_IdAndStatusOrderByCreatedAtDesc(restaurant.getId(), "REQUESTED");
    }

    public List<CompanySubscription> getCompanyRequests(Company company) {
        return companySubscriptionRepository.findByCompany(company);
    }

    public List<CompanySubscription> getCompanySubscriptions(Company company) {
        return companySubscriptionRepository.findByCompanyAndStatusOrderByStartDateDesc(company, "ACTIVE");
    }

    public CompanySubscription getCompanySubscription(Long subscriptionId) {
        return companySubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
    }

    @Transactional
    public CompanyOrder placeOrder(Company company, Long menuItemId, LocalDate orderDate,
            Integer quantity, String deliveryAddress, String specialInstructions) {
        // Check if company has active subscription
        Optional<CompanySubscription> activeSubscription = getActiveSubscription(company);
        if (activeSubscription.isEmpty()) {
            throw new IllegalStateException("No active subscription found. Please subscribe to a package first.");
        }

        CompanySubscription subscription = activeSubscription.get();
        SubscriptionPackage subscriptionPackage = subscription.getSubscriptionPackage();

        // Validate quantity matches subscription package
        if (!quantity.equals(subscriptionPackage.getNumberOfPeople())) {
            throw new IllegalArgumentException(
                    "Quantity must match your subscription package (" +
                            subscriptionPackage.getNumberOfPeople() + " people)");
        }

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

        if (!menuItem.isAvailable()) {
            throw new IllegalArgumentException("This item is currently unavailable");
        }

        // Validate that the menu item is in the subscription's selected items
        List<SubscriptionItem> subscriptionItems = subscriptionItemRepository.findBySubscription(subscription);
        boolean itemInSubscription = subscriptionItems.stream()
                .anyMatch(item -> item.getMenuItem().getId().equals(menuItemId));

        if (!itemInSubscription) {
            throw new IllegalArgumentException(
                    "This menu item is not included in your subscription. Please select from your subscription items.");
        }

        // Calculate total amount
        BigDecimal unitPrice = menuItem.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        CompanyOrder order = new CompanyOrder();
        order.setCompany(company);
        order.setMenuItem(menuItem);
        order.setOrderDate(orderDate);
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setDeliveryAddress(deliveryAddress.trim());
        order.setSpecialInstructions(specialInstructions);
        return companyOrderRepository.save(order);
    }

    @Transactional
    public void placeDailyOrder(Company company, LocalDate orderDate, String deliveryAddress,
            String specialInstructions) {
        // Check if company has active subscription
        Optional<CompanySubscription> activeSubscription = getActiveSubscription(company);
        if (activeSubscription.isEmpty()) {
            throw new IllegalStateException("No active subscription found. Please subscribe to a package first.");
        }

        CompanySubscription subscription = activeSubscription.get();
        SubscriptionPackage subscriptionPackage = subscription.getSubscriptionPackage();
        Integer quantity = subscription.getPeopleCount() != null ? subscription.getPeopleCount()
                : subscriptionPackage.getNumberOfPeople();

        // Check if orders already exist for this date
        List<CompanyOrder> existingOrders = companyOrderRepository.findByCompanyAndOrderDate(company, orderDate);
        if (!existingOrders.isEmpty()) {
            throw new IllegalStateException("Orders for " + orderDate + " have already been placed.");
        }

        // Validate items exist in subscription
        List<SubscriptionItem> subscriptionItems = subscriptionItemRepository.findBySubscription(subscription);
        if (subscriptionItems.isEmpty()) {
            throw new IllegalStateException("Your subscription has no items configured.");
        }

        // Create an order for EACH item in the subscription
        for (SubscriptionItem subItem : subscriptionItems) {
            MenuItem menuItem = subItem.getMenuItem();

            if (!menuItem.isAvailable()) {
                throw new IllegalStateException("Item " + menuItem.getName() + " is currently unavailable.");
            }

            // Calculate total amount per item
            BigDecimal totalAmount = menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));

            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new IllegalArgumentException("Delivery address is required");
            }

            CompanyOrder order = new CompanyOrder();
            order.setCompany(company);
            order.setMenuItem(menuItem);
            order.setOrderDate(orderDate);
            order.setQuantity(quantity);
            order.setTotalAmount(totalAmount);
            order.setDeliveryAddress(deliveryAddress.trim());
            order.setSpecialInstructions(specialInstructions);
            order.setStatus("PENDING");

            companyOrderRepository.save(order);
        }
    }

    public List<CompanyOrder> getCompanyOrders(Company company) {
        return companyOrderRepository.findByCompanyOrderByOrderDateDesc(company);
    }

    public List<CompanyOrder> getCompanyOrdersByDate(Company company, LocalDate orderDate) {
        return companyOrderRepository.findByCompanyAndOrderDate(company, orderDate);
    }

    public List<CompanyOrder> getCompanyOrdersByStatus(Company company, String status) {
        return companyOrderRepository.findByCompanyAndStatus(company, status);
    }

    @Transactional
    public CompanyOrder updateOrderStatus(Long orderId, String status) {
        CompanyOrder order = companyOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(status);
        if ("DELIVERED".equals(status)) {
            order.setDeliveryTime(LocalDateTime.now());
        }
        return companyOrderRepository.save(order);
    }

    @Transactional
    @SuppressWarnings("null")
    public void deleteAllCompanyData(Company company) {
        // 1. Delete all orders
        List<CompanyOrder> orders = companyOrderRepository.findByCompany(company);
        companyOrderRepository.deleteAll(orders);

        // 2. Delete all subscriptions and their items
        List<CompanySubscription> subscriptions = companySubscriptionRepository.findByCompany(company);
        for (CompanySubscription sub : subscriptions) {
            subscriptionItemRepository.deleteBySubscription(sub);
        }
        companySubscriptionRepository.deleteAll(subscriptions);

        // 3. Delete company
        companyRepository.delete(company);
    }
}
