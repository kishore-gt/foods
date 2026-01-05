package com.srFoodDelivery.Controller;

import java.util.List;

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

import com.srFoodDelivery.dto.MenuForm;
import com.srFoodDelivery.dto.MenuItemForm;
import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.RestaurantTag;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.ChefProfileService;
import com.srFoodDelivery.service.MenuItemService;
import com.srFoodDelivery.service.MenuService;
import com.srFoodDelivery.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Controller
@RequestMapping("/chef")
public class ChefController {

    private final ChefProfileService chefProfileService;
    private final MenuService menuService;
    private final MenuItemService menuItemService;
    private final OrderService orderService;

    public ChefController(ChefProfileService chefProfileService,
                          MenuService menuService,
                          MenuItemService menuItemService,
                          OrderService orderService) {
        this.chefProfileService = chefProfileService;
        this.menuService = menuService;
        this.menuItemService = menuItemService;
        this.orderService = orderService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        model.addAttribute("profile", profile);
        model.addAttribute("menus", menuService.findByChefProfile(profile));
        // Chef feature removed - orders no longer supported for chefs
        model.addAttribute("orders", java.util.Collections.emptyList());
        return "chef/dashboard";
    }

    @GetMapping("/profile")
    public String showProfileForm(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", ProfileForm.from(profile));
        }
        return "chef/profile-form";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "chef/profile-form";
        }

        chefProfileService.updateProfile(principal.getUser(), form.getBio(), form.getSpeciality(), form.getLocation());
        return "redirect:/chef/dashboard?profileUpdated";
    }

    @GetMapping("/menus")
    public String listMenus(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        model.addAttribute("menus", menuService.findByChefProfile(profile));
        model.addAttribute("profile", profile);
        return "chef/menus";
    }

    @GetMapping("/menus/new")
    public String newMenu(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        MenuForm form = new MenuForm();
        form.setType(Menu.Type.CHEF.name());
        form.setChefProfileId(profile.getId());
        model.addAttribute("profile", profile);
        model.addAttribute("menuForm", form);
        return "chef/menu-form";
    }

    @PostMapping("/menus")
    public String createMenu(@AuthenticationPrincipal CustomUserDetails principal,
                             @Valid @ModelAttribute("menuForm") MenuForm form,
                             BindingResult bindingResult,
                             Model model) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        form.setType(Menu.Type.CHEF.name());
        form.setChefProfileId(profile.getId());

        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", profile);
            return "chef/menu-form";
        }

        menuService.createMenu(form);
        return "redirect:/chef/menus";
    }

    @GetMapping("/menus/{menuId}/edit")
    public String editMenu(@PathVariable Long menuId,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           Model model) {
        Menu menu = requireChefMenu(menuId, principal.getUser());
        MenuForm form = toMenuForm(menu);
        model.addAttribute("menuId", menu.getId());
        model.addAttribute("profile", menu.getChefProfile());
        model.addAttribute("menuForm", form);
        return "chef/menu-form";
    }

    @PostMapping("/menus/{menuId}/edit")
    public String updateMenu(@PathVariable Long menuId,
                             @AuthenticationPrincipal CustomUserDetails principal,
                             @Valid @ModelAttribute("menuForm") MenuForm form,
                             BindingResult bindingResult,
                             Model model) {
        Menu menu = requireChefMenu(menuId, principal.getUser());
        form.setType(Menu.Type.CHEF.name());
        form.setChefProfileId(menu.getChefProfile().getId());

        if (bindingResult.hasErrors()) {
            model.addAttribute("menuId", menuId);
            model.addAttribute("profile", menu.getChefProfile());
            return "chef/menu-form";
        }

        menuService.updateMenu(menuId, form);
        return "redirect:/chef/menus";
    }

    @PostMapping("/menus/{menuId}/delete")
    public String deleteMenu(@PathVariable Long menuId,
                             @AuthenticationPrincipal CustomUserDetails principal) {
        Menu menu = requireChefMenu(menuId, principal.getUser());
        menuService.deleteMenu(menuId);
        return "redirect:/chef/menus";
    }

    @GetMapping("/menus/{menuId}/items")
    public String listMenuItems(@PathVariable Long menuId,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {
        Menu menu = requireChefMenu(menuId, principal.getUser());
        List<MenuItem> items = menuItemService.getItemsForMenu(menuId);
        model.addAttribute("menu", menu);
        model.addAttribute("items", items);
        return "chef/menu-items";
    }

    @GetMapping("/menus/{menuId}/items/new")
    public String newMenuItem(@PathVariable Long menuId,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              Model model) {
        Menu menu = requireChefMenu(menuId, principal.getUser());
        if (!model.containsAttribute("menuItemForm")) {
            model.addAttribute("menuItemForm", new MenuItemForm());
        }
        addMenuItemTagOptions(model);
        model.addAttribute("menu", menu);
        return "chef/menu-item-form";
    }

    @PostMapping("/menus/{menuId}/items")
    public String createMenuItem(@PathVariable Long menuId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 @Valid @ModelAttribute("menuItemForm") MenuItemForm form,
                                 BindingResult bindingResult,
                                 Model model) {
        Menu menu = requireChefMenu(menuId, principal.getUser());
        if (bindingResult.hasErrors()) {
            model.addAttribute("menu", menu);
            addMenuItemTagOptions(model);
            return "chef/menu-item-form";
        }

        menuItemService.addMenuItem(menuId, form);
        return "redirect:/chef/menus/" + menuId + "/items";
    }

    @GetMapping("/items/{itemId}/edit")
    public String editMenuItem(@PathVariable Long itemId,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {
        MenuItem item = requireChefMenuItem(itemId, principal.getUser());
        MenuItemForm form = toMenuItemForm(item);
        model.addAttribute("itemId", item.getId());
        model.addAttribute("menu", item.getMenu());
        model.addAttribute("menuItemForm", form);
        addMenuItemTagOptions(model);
        return "chef/menu-item-form";
    }

    @PostMapping("/items/{itemId}/edit")
    public String updateMenuItem(@PathVariable Long itemId,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 @Valid @ModelAttribute("menuItemForm") MenuItemForm form,
                                 BindingResult bindingResult,
                                 Model model) {
        MenuItem item = requireChefMenuItem(itemId, principal.getUser());
        if (bindingResult.hasErrors()) {
            model.addAttribute("itemId", itemId);
            model.addAttribute("menu", item.getMenu());
            addMenuItemTagOptions(model);
            return "chef/menu-item-form";
        }

        menuItemService.updateMenuItem(itemId, form);
        return "redirect:/chef/menus/" + item.getMenu().getId() + "/items";
    }

    @PostMapping("/items/{itemId}/delete")
    public String deleteMenuItem(@PathVariable Long itemId,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        MenuItem item = requireChefMenuItem(itemId, principal.getUser());
        Long menuId = item.getMenu().getId();
        menuItemService.deleteMenuItem(itemId);
        return "redirect:/chef/menus/" + menuId + "/items";
    }

    private void addMenuItemTagOptions(Model model) {
        model.addAttribute("availableTags", RestaurantTag.allTags());
    }

    private Menu requireChefMenu(Long menuId, User chef) {
        Menu menu = menuService.getMenu(menuId);
        if (menu.getChefProfile() == null || !menu.getChefProfile().getChef().getId().equals(chef.getId())) {
            throw new SecurityException("Access denied");
        }
        return menu;
    }

    private MenuItem requireChefMenuItem(Long itemId, User chef) {
        // Use getMenuItemForEdit when loading for edit form to preserve imageUrl
        MenuItem item = menuItemService.getMenuItemForEdit(itemId);
        if (item.getMenu().getChefProfile() == null || !item.getMenu().getChefProfile().getChef().getId().equals(chef.getId())) {
            throw new SecurityException("Access denied");
        }
        return item;
    }

    public static class ProfileForm {

        @Size(max = 500)
        private String bio;

        @Size(max = 150)
        private String speciality;

        @Size(max = 200)
        private String location;

        public static ProfileForm from(ChefProfile profile) {
            ProfileForm form = new ProfileForm();
            form.setBio(profile.getBio());
            form.setSpeciality(profile.getSpeciality());
            form.setLocation(profile.getLocation());
            return form;
        }

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }

        public String getSpeciality() {
            return speciality;
        }

        public void setSpeciality(String speciality) {
            this.speciality = speciality;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    private MenuForm toMenuForm(Menu menu) {
        MenuForm form = new MenuForm();
        form.setTitle(menu.getTitle());
        form.setDescription(menu.getDescription());
        form.setType(Menu.Type.CHEF.name());
        if (menu.getChefProfile() != null) {
            form.setChefProfileId(menu.getChefProfile().getId());
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

    @GetMapping("/orders")
    public String listOrders(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        model.addAttribute("profile", profile);
        // Chef feature removed - orders no longer supported for chefs
        model.addAttribute("orders", java.util.Collections.emptyList());
        return "chef/orders";
    }

    @PostMapping("/orders/{orderId}/status")
    public String updateOrderStatus(@PathVariable Long orderId,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        ChefProfile profile = chefProfileService.getOrCreateProfile(principal.getUser());
        Order order = orderService.getOrder(orderId);
        
        // Verify the order belongs to this chef
        if (order.getChefProfile() == null || !order.getChefProfile().getId().equals(profile.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied");
            return "redirect:/chef/orders";
        }
        
        orderService.updateStatus(orderId, status);
        redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully");
        return "redirect:/chef/orders";
    }
}
