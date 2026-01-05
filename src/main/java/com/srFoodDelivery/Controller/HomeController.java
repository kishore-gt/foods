package com.srFoodDelivery.Controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.srFoodDelivery.model.SiteMode;

import com.srFoodDelivery.service.MenuItemService;
import com.srFoodDelivery.service.OfferService;
import com.srFoodDelivery.service.RestaurantService;
import com.srFoodDelivery.service.SiteModeManager;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final MenuItemService menuItemService;
    private final OfferService offerService;
    private final RestaurantService restaurantService;
    private final SiteModeManager siteModeManager;

    public HomeController(MenuItemService menuItemService,
                          OfferService offerService,
                          RestaurantService restaurantService,
                          SiteModeManager siteModeManager) {
        this.menuItemService = menuItemService;
        this.offerService = offerService;
        this.restaurantService = restaurantService;
        this.siteModeManager = siteModeManager;
    }

    @GetMapping("/")
    public String index(Model model,
                        HttpSession session,
                        @RequestParam(value = "mode", required = false) String mode) {
        SiteMode siteMode = siteModeManager.resolveMode(mode, session);
        
        // Mode-aware datasets
        List<com.srFoodDelivery.model.Offer> activeOffers = offerService.getActiveOffersForMode(siteMode);
        List<String> categories = menuItemService.getAllCategoriesForMode(siteMode);
        List<com.srFoodDelivery.model.MenuItem> popularItems = menuItemService.getAvailableItemsForMode(siteMode)
                .stream()
                .limit(12)
                .collect(Collectors.toList());
        
        // Get restaurants/cafes based on mode
        List<com.srFoodDelivery.model.Restaurant> restaurants = restaurantService.findByMode(siteMode);
        
        // Create a map of category to image URL for the template
        java.util.Map<String, String> categoryImages = new java.util.HashMap<>();
        for (String category : categories) {
            categoryImages.put(category, getCategoryImageUrl(category));
        }
        
        model.addAttribute("activeOffers", activeOffers);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryImages", categoryImages);
        model.addAttribute("popularItems", popularItems);
        model.addAttribute("restaurants", restaurants);
        model.addAttribute("restaurantCount", restaurants.size());
        model.addAttribute("siteMode", siteMode);
        model.addAttribute("modeDisplayName", siteMode.getDisplayName());
        model.addAttribute("modeDescription", siteMode.getDescription());
        
        return "index";
    }
    
    /**
     * Helper method to get category image URL
     */
    public static String getCategoryImageUrl(String category) {
        if (category == null) {
            return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80";
        }
        switch (category) {
            case "STARTERS":
                return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80";
            case "DESSERTS":
                return "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=400&q=80";
            case "BEVERAGES":
                return "https://images.unsplash.com/photo-1544145945-f90425340c7e?w=400&q=80";
            case "SNACKS":
                return "https://images.unsplash.com/photo-1562967914-608f82629710?w=400&q=80";
            case "RICE":
                return "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400&q=80";
            case "BREADS":
                return "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400&q=80";
            case "CURRIES":
                return "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400&q=80";
            case "SOUPS":
                return "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&q=80";
            case "SALADS":
                return "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&q=80";
            default:
                return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80";
        }
    }
}
