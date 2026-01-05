package com.srFoodDelivery.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.MenuForm;
import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.repository.ChefProfileRepository;
import com.srFoodDelivery.repository.MenuRepository;
import com.srFoodDelivery.repository.RestaurantRepository;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final RestaurantRepository restaurantRepository;
    private final ChefProfileRepository chefProfileRepository;

    public MenuService(MenuRepository menuRepository,
                       RestaurantRepository restaurantRepository,
                       ChefProfileRepository chefProfileRepository) {
        this.menuRepository = menuRepository;
        this.restaurantRepository = restaurantRepository;
        this.chefProfileRepository = chefProfileRepository;
    }

    @Transactional
    public Menu createMenu(MenuForm form) {
        Menu menu = new Menu();
        applyForm(menu, form);
        return menuRepository.save(menu);
    }

    public Menu getMenu(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found"));
    }

    @Transactional
    public Menu updateMenu(Long id, MenuForm form) {
        Menu menu = getMenu(id);
        applyForm(menu, form);
        return menu;
    }

    @Transactional
    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }

    public List<Menu> findByRestaurant(Restaurant restaurant) {
        return menuRepository.findByRestaurant(restaurant);
    }

    public List<Menu> findByChefProfile(ChefProfile chefProfile) {
        return menuRepository.findByChefProfile(chefProfile);
    }

    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    public List<Menu> findByType(Menu.Type type) {
        return menuRepository.findByType(type);
    }

    private void applyForm(Menu menu, MenuForm form) {
        menu.setTitle(form.getTitle());
        menu.setDescription(form.getDescription());
        menu.setType(Menu.Type.valueOf(form.getType().toUpperCase()));

        menu.setRestaurant(null);
        menu.setChefProfile(null);

        if (menu.getType() == Menu.Type.RESTAURANT) {
            if (form.getRestaurantId() == null) {
                throw new IllegalArgumentException("Restaurant ID required for restaurant menus");
            }
            Restaurant restaurant = restaurantRepository.findById(form.getRestaurantId())
                    .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            menu.setRestaurant(restaurant);
        } else {
            if (form.getChefProfileId() == null) {
                throw new IllegalArgumentException("Chef profile ID required for chef menus");
            }
            ChefProfile chefProfile = chefProfileRepository.findById(form.getChefProfileId())
                    .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
            menu.setChefProfile(chefProfile);
        }
    }
}
