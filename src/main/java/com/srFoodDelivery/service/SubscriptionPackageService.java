package com.srFoodDelivery.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.SubscriptionPackageForm;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.PackageMenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubscriptionPackage;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.PackageMenuItemRepository;
import com.srFoodDelivery.repository.SubscriptionPackageRepository;

@Service
public class SubscriptionPackageService {

    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final PackageMenuItemRepository packageMenuItemRepository;
    private final MenuItemRepository menuItemRepository;

    public SubscriptionPackageService(
            SubscriptionPackageRepository subscriptionPackageRepository,
            PackageMenuItemRepository packageMenuItemRepository,
            MenuItemRepository menuItemRepository) {
        this.subscriptionPackageRepository = subscriptionPackageRepository;
        this.packageMenuItemRepository = packageMenuItemRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public SubscriptionPackage createPackage(Restaurant restaurant, SubscriptionPackageForm form) {
        SubscriptionPackage pkg = new SubscriptionPackage();
        pkg.setRestaurant(restaurant);
        pkg.setName(form.getName());
        pkg.setDescription(form.getDescription());
        pkg.setNumberOfPeople(form.getNumberOfPeople());
        pkg.setMealType(form.getMealType());
        pkg.setIsActive(true);

        // Calculate and enforce price
        BigDecimal calculatedPrice = calculateDiscountedPrice(form.getMenuItemIds(), form.getNumberOfPeople(),
                restaurant);
        pkg.setPrice(calculatedPrice);

        SubscriptionPackage savedPackage = subscriptionPackageRepository.save(pkg);

        // Add menu items to package
        if (form.getMenuItemIds() != null && !form.getMenuItemIds().isEmpty()) {
            addMenuItemsToPackage(savedPackage, form.getMenuItemIds(), restaurant);
        }

        return savedPackage;
    }

    @Transactional
    public SubscriptionPackage updatePackage(Long packageId, Restaurant restaurant, SubscriptionPackageForm form) {
        SubscriptionPackage pkg = subscriptionPackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));

        if (!pkg.getRestaurant().getId().equals(restaurant.getId())) {
            throw new SecurityException("Access denied");
        }

        pkg.setName(form.getName());
        pkg.setDescription(form.getDescription());
        pkg.setNumberOfPeople(form.getNumberOfPeople());
        pkg.setMealType(form.getMealType());

        // Calculate and enforce price
        BigDecimal calculatedPrice = calculateDiscountedPrice(form.getMenuItemIds(), form.getNumberOfPeople(),
                restaurant);
        pkg.setPrice(calculatedPrice);

        SubscriptionPackage savedPackage = subscriptionPackageRepository.save(pkg);

        // Remove existing menu items
        packageMenuItemRepository.deleteBySubscriptionPackage(savedPackage);

        // Add new menu items
        if (form.getMenuItemIds() != null && !form.getMenuItemIds().isEmpty()) {
            addMenuItemsToPackage(savedPackage, form.getMenuItemIds(), restaurant);
        }

        return savedPackage;
    }

    private BigDecimal calculateDiscountedPrice(List<Long> menuItemIds, Integer numberOfPeople, Restaurant restaurant) {
        if (menuItemIds == null || menuItemIds.isEmpty() || numberOfPeople == null || numberOfPeople <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (Long menuItemId : menuItemIds) {
            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));

            // Verify menu item belongs to the restaurant
            if (menuItem.getMenu() == null || menuItem.getMenu().getRestaurant() == null
                    || !menuItem.getMenu().getRestaurant().getId().equals(restaurant.getId())) {
                throw new SecurityException("Menu item does not belong to this restaurant");
            }

            itemsTotal = itemsTotal.add(menuItem.getPrice());
        }

        // Formula: (ItemsTotal * People) * 0.8
        BigDecimal baseTotal = itemsTotal.multiply(BigDecimal.valueOf(numberOfPeople));
        return baseTotal.multiply(BigDecimal.valueOf(0.8)); // Apply 20% discount
    }

    private void addMenuItemsToPackage(SubscriptionPackage pkg, List<Long> menuItemIds, Restaurant restaurant) {
        for (Long menuItemId : menuItemIds) {
            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));

            // Check if already exists
            if (!packageMenuItemRepository.existsBySubscriptionPackageAndMenuItemId(pkg, menuItemId)) {
                PackageMenuItem packageMenuItem = new PackageMenuItem(pkg, menuItem);
                packageMenuItemRepository.save(packageMenuItem);
            }
        }
    }

    public List<MenuItem> getPackageMenuItems(SubscriptionPackage subscriptionPackage) {
        List<PackageMenuItem> packageMenuItems = packageMenuItemRepository
                .findBySubscriptionPackage(subscriptionPackage);
        return packageMenuItems.stream()
                .map(PackageMenuItem::getMenuItem)
                .collect(Collectors.toList());
    }

    public SubscriptionPackage getPackageById(Long packageId) {
        return subscriptionPackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));
    }

    @Transactional
    public void deletePackage(Long packageId, Restaurant restaurant) {
        SubscriptionPackage pkg = subscriptionPackageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("Package not found"));

        if (!pkg.getRestaurant().getId().equals(restaurant.getId())) {
            throw new SecurityException("Access denied");
        }

        // Soft delete by setting isActive to false
        pkg.setIsActive(false);
        subscriptionPackageRepository.save(pkg);
    }
}
