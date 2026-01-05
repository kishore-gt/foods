package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.Menu.Type;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.ChefProfile;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByRestaurant(Restaurant restaurant);
    List<Menu> findByChefProfile(ChefProfile chefProfile);
    List<Menu> findByType(Type type);
}
