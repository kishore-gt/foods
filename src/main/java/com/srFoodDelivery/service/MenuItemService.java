package com.srFoodDelivery.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.MenuItemForm;
import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.SiteMode;
import com.srFoodDelivery.repository.MenuItemRepository;
import com.srFoodDelivery.repository.MenuRepository;

@Service
@Transactional(readOnly = true)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuRepository menuRepository;
    private final ImageService imageService;

    public MenuItemService(MenuItemRepository menuItemRepository, MenuRepository menuRepository, ImageService imageService) {
        this.menuItemRepository = menuItemRepository;
        this.menuRepository = menuRepository;
        this.imageService = imageService;
    }

    public List<MenuItem> getAvailableItems() {
        List<MenuItem> items = menuItemRepository.findByAvailableTrue();
        ensureImageUrls(items);
        return items;
    }

    public List<MenuItem> getAvailableItemsForMode(SiteMode siteMode) {
        List<MenuItem> items = getAvailableItems();
        return filterByMode(items, siteMode);
    }

    public List<MenuItem> getAvailableItemsByTags(Set<String> tags) {
        List<MenuItem> items;
        if (tags == null || tags.isEmpty()) {
            items = getAvailableItems();
        } else {
            List<MenuItem> candidates = menuItemRepository.findByAvailableTrueAndTagsIn(tags);
            items = candidates.stream()
                    .filter(item -> {
                        Set<String> itemTags = item.getTags();
                        if (itemTags == null) {
                            itemTags = Collections.emptySet();
                        }
                        return itemTags.containsAll(tags);
                    })
                    .collect(Collectors.toList());
            ensureImageUrls(items);
        }
        return items;
    }
    
    private void ensureImageUrls(List<MenuItem> items) {
        for (MenuItem item : items) {
            if (item.getImageUrl() == null || item.getImageUrl().trim().isEmpty()) {
                item.setImageUrl(imageService.getImageUrlForFood(item.getName()));
            }
        }
    }

    @Transactional
    public MenuItem addMenuItem(Long menuId, MenuItemForm form) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found"));

        MenuItem item = new MenuItem();
        item.setMenu(menu);
        applyForm(item, form);
        return menuItemRepository.save(item);
    }

    public MenuItem getMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        if (item.getImageUrl() == null || item.getImageUrl().trim().isEmpty()) {
            item.setImageUrl(imageService.getImageUrlForFood(item.getName()));
        }
        return item;
    }
    
    /**
     * Get menu item for editing without calling ensureImageUrl
     * This preserves the user's custom imageUrl
     */
    public MenuItem getMenuItemForEdit(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        // Don't call ensureImageUrl here - preserve whatever is in the database
        return item;
    }

    @Transactional
    public MenuItem updateMenuItem(Long id, MenuItemForm form) {
        // Get item directly without calling getMenuItem to avoid ensureImageUrl interference
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        applyForm(item, form);
        return menuItemRepository.save(item);
    }

    @Transactional
    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    @Transactional
    public int deleteAllCafeItems() {
        List<MenuItem> cafeItems = menuItemRepository.findByCafeRestaurants();
        int count = cafeItems.size();
        menuItemRepository.deleteAll(cafeItems);
        return count;
    }

    public List<MenuItem> getItemsForMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found"));
        List<MenuItem> items = menuItemRepository.findByMenu(menu);
        ensureImageUrls(items);
        return items;
    }

    public List<MenuItem> getItemsByCategory(String category) {
        List<MenuItem> items = menuItemRepository.findByAvailableTrueAndCategory(category);
        ensureImageUrls(items);
        return items;
    }

    public List<MenuItem> getItemsByCategoryForMode(String category, SiteMode siteMode) {
        List<MenuItem> items = getItemsByCategory(category);
        return filterByMode(items, siteMode);
    }

    public List<MenuItem> getItemsByCategoryAndVeg(String category, boolean isVeg) {
        List<MenuItem> items = menuItemRepository.findByAvailableTrueAndCategoryAndIsVeg(category, isVeg);
        ensureImageUrls(items);
        return items;
    }

    public List<String> getAllCategories() {
        return menuItemRepository.findDistinctCategories();
    }

    public List<String> getAllCategoriesForMode(SiteMode siteMode) {
        List<MenuItem> items = getAvailableItemsForMode(siteMode);
        return items.stream()
                .map(MenuItem::getCategory)
                .filter(category -> category != null && !category.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<MenuItem> filterByMode(List<MenuItem> items, SiteMode siteMode) {
        if (siteMode == null) {
            // Default to restaurant mode - show only non-cafe items
            return items.stream()
                    .filter(item -> item.getMenu() != null &&
                            item.getMenu().getRestaurant() != null &&
                            !item.getMenu().getRestaurant().isCafeLounge())
                    .collect(Collectors.toList());
        }
        
        if (siteMode.isCafeMode()) {
            // Cafe mode: show only cafe items
            return items.stream()
                    .filter(item -> item.getMenu() != null &&
                            item.getMenu().getRestaurant() != null &&
                            item.getMenu().getRestaurant().isCafeLounge())
                    .collect(Collectors.toList());
        } else {
            // Restaurant mode: show only non-cafe items
            return items.stream()
                    .filter(item -> item.getMenu() != null &&
                            item.getMenu().getRestaurant() != null &&
                            !item.getMenu().getRestaurant().isCafeLounge())
                    .collect(Collectors.toList());
        }
    }

    private void applyForm(MenuItem item, MenuItemForm form) {
        item.setName(form.getName());
        item.setDescription(form.getDescription());
        item.setPrice(form.getPrice());
        item.setAvailable(form.isAvailable());
        item.setTags(form.getTags());
        
        // Set category and veg status if provided in form
        if (form.getCategory() != null) {
            item.setCategory(form.getCategory());
        }
        if (form.getIsVeg() != null) {
            item.setVeg(form.getIsVeg());
        }
        
        // Handle imageUrl: Use form's value if provided, otherwise keep existing or auto-generate
        String formImageUrl = form.getImageUrl();
        if (formImageUrl != null && !formImageUrl.trim().isEmpty()) {
            // User provided a URL in the form - use it (trimmed)
            item.setImageUrl(formImageUrl.trim());
        } else if (item.getImageUrl() == null || item.getImageUrl().trim().isEmpty()) {
            // No URL in form and no existing URL - auto-generate
            item.setImageUrl(imageService.getImageUrlForFood(form.getName()));
        }
        // If form is empty but item already has a URL, keep the existing URL
    }
}
