package com.srFoodDelivery.main;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.MenuForm;
import com.srFoodDelivery.dto.MenuItemForm;
import com.srFoodDelivery.dto.RestaurantForm;
import com.srFoodDelivery.model.RestaurantTag;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.model.UserRole;
import com.srFoodDelivery.repository.UserRepository;
import com.srFoodDelivery.service.ChefProfileService;
import com.srFoodDelivery.service.MenuItemService;
import com.srFoodDelivery.service.MenuService;
import com.srFoodDelivery.service.RestaurantService;

@Configuration
public class DataInitializer {

	@Bean
	CommandLineRunner initData(UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			RestaurantService restaurantService,
			MenuService menuService,
			MenuItemService menuItemService,
			ChefProfileService chefProfileService) {
		return args -> seed(userRepository, passwordEncoder, restaurantService, menuService, menuItemService,
				chefProfileService);
	}

	@Transactional
	void seed(UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			RestaurantService restaurantService,
			MenuService menuService,
			MenuItemService menuItemService,
			ChefProfileService chefProfileService) {
		// Seed default users if they don't exist
		User admin = ensureUser(userRepository, passwordEncoder,
				"admin@example.com", "Admin User", "password", UserRole.ADMIN, "1234567890");

		User owner = ensureUser(userRepository, passwordEncoder,
				"owner1@example.com", "Owner One", "password", UserRole.OWNER, "1234567891");
		User cafeOwner = ensureUser(userRepository, passwordEncoder,
				"cafeowner@example.com", "Cafe Owner", "password", UserRole.CAFE_OWNER, "1234567894");
		User chef = ensureUser(userRepository, passwordEncoder,
				"chef1@example.com", "Chef One", "password", UserRole.CHEF, "1234567892");
		User customer = ensureUser(userRepository, passwordEncoder,
				"customer1@example.com", "Customer One", "password", UserRole.CUSTOMER, "1234567893");

		// Owner sample: restaurant + menu + items
		if (restaurantService.findByOwner(owner).isEmpty()) {
			RestaurantForm rForm = new RestaurantForm();
			rForm.setName("Owner's Diner");
			rForm.setDescription("Cozy place with comfort food");
			rForm.setAddress("123 Main St");
			rForm.setContactNumber("123-456-7890");
			rForm.setBusinessType("RESTAURANT");
			rForm.setOpeningTime("09:00");
			rForm.setClosingTime("22:00");
			var restaurant = restaurantService.createRestaurant(rForm, owner);

			MenuForm mForm = new MenuForm();
			mForm.setTitle("Main Menu");
			mForm.setDescription("House specials");
			mForm.setType("RESTAURANT");
			mForm.setRestaurantId(restaurant.getId());
			var menu = menuService.createMenu(mForm);

			MenuItemForm i1 = new MenuItemForm();
			i1.setName("Burger");
			i1.setDescription("Beef burger with fries");
			i1.setPrice(new BigDecimal("9.99"));
			i1.setAvailable(true);
			i1.setTags(Set.of(RestaurantTag.LUNCH, RestaurantTag.NONVEG));
			menuItemService.addMenuItem(menu.getId(), i1);

			MenuItemForm i2 = new MenuItemForm();
			i2.setName("Salad");
			i2.setDescription("Fresh garden salad");
			i2.setPrice(new BigDecimal("6.50"));
			i2.setAvailable(true);
			i2.setTags(Set.of(RestaurantTag.LUNCH, RestaurantTag.VEG));
			menuItemService.addMenuItem(menu.getId(), i2);
		}

		if (restaurantService.findByOwner(cafeOwner).isEmpty()) {
			RestaurantForm cForm = new RestaurantForm();
			cForm.setName("Caffeine House");
			cForm.setDescription("Artisanal coffees, signature brews, and cozy nooks.");
			cForm.setAddress("77 Brew Street");
			cForm.setContactNumber("321-654-0987");
			cForm.setBusinessType("CAFE");
			cForm.setOpeningTime("07:00");
			cForm.setClosingTime("20:00");
			var cafe = restaurantService.createRestaurant(cForm, cafeOwner);

			MenuForm cafeMenu = new MenuForm();
			cafeMenu.setTitle("Cafe Classics");
			cafeMenu.setDescription("Handcrafted beverages and bites");
			cafeMenu.setType("RESTAURANT");
			cafeMenu.setRestaurantId(cafe.getId());
			var createdMenu = menuService.createMenu(cafeMenu);

			MenuItemForm latte = new MenuItemForm();
			latte.setName("Hazelnut Latte");
			latte.setDescription("Velvety espresso with hazelnut cream.");
			latte.setPrice(new BigDecimal("5.50"));
			latte.setAvailable(true);
			latte.setTags(Set.of(RestaurantTag.BREAKFAST, RestaurantTag.VEG));
			menuItemService.addMenuItem(createdMenu.getId(), latte);

			MenuItemForm croissant = new MenuItemForm();
			croissant.setName("Almond Croissant");
			croissant.setDescription("Freshly baked flaky pastry filled with almond cream.");
			croissant.setPrice(new BigDecimal("4.00"));
			croissant.setAvailable(true);
			croissant.setTags(Set.of(RestaurantTag.BREAKFAST, RestaurantTag.VEG));
			menuItemService.addMenuItem(createdMenu.getId(), croissant);
		}

		// Chef sample: profile + menu + item
		var profile = chefProfileService.getOrCreateProfile(chef);
		if (menuService.findByChefProfile(profile).isEmpty()) {
			MenuForm mForm = new MenuForm();
			mForm.setTitle("Chef Specials");
			mForm.setDescription("Signature dishes by Chef One");
			mForm.setType("CHEF");
			mForm.setChefProfileId(profile.getId());
			var menu = menuService.createMenu(mForm);

			MenuItemForm i1 = new MenuItemForm();
			i1.setName("Truffle Pasta");
			i1.setDescription("Pasta with truffle cream");
			i1.setPrice(new BigDecimal("14.00"));
			i1.setAvailable(true);
			i1.setTags(Set.of(RestaurantTag.DINNER, RestaurantTag.NONVEG));
			menuItemService.addMenuItem(menu.getId(), i1);
		}
	}

	private User ensureUser(UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			String email, String fullName, String rawPassword, String role, String phoneNumber) {
		return userRepository.findByEmail(email.toLowerCase())
				.orElseGet(() -> {
					User u = new User();
					u.setEmail(email.toLowerCase());
					u.setFullName(fullName);
					u.setPasswordHash(passwordEncoder.encode(rawPassword));
					u.setRole(role);
					u.setPhoneNumber(phoneNumber);
					return userRepository.save(u);
				});
	}
}
