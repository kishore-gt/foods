package com.srFoodDelivery.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.Cart;
import com.srFoodDelivery.model.CartItem;
import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.CartRepository;
import com.srFoodDelivery.repository.MenuItemRepository;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final MenuItemRepository menuItemRepository;

    public CartService(CartRepository cartRepository,
            MenuItemRepository menuItemRepository) {
        this.cartRepository = cartRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Cart> findCart(User user) {
        return cartRepository.findByUser(user);
    }

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setTotalAmount(BigDecimal.ZERO);
                    return cartRepository.save(cart);
                });
    }

    public Cart addItem(User user, Long menuItemId, int quantity) {
        if (quantity < 1) {
            quantity = 1;
        }

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        if (!menuItem.isAvailable()) {
            throw new IllegalArgumentException("This item is currently unavailable.");
        }

        Restaurant itemRestaurant = extractRestaurant(menuItem);
        if (itemRestaurant == null) {
            throw new IllegalArgumentException("Menu item is not associated with a restaurant.");
        }

        Cart cart = getOrCreateCart(user);

        // Allow items from multiple restaurants - no restriction
        // The cart's restaurant_id is kept for backward compatibility
        // When the cart is empty or has items from one restaurant, set the cart's
        // restaurant
        // for backward compatibility with legacy order system
        if (cart.getItems().isEmpty()) {
            // First item - set the restaurant for backward compatibility
            cart.setRestaurant(itemRestaurant);
            cart.setChefProfile(null);
        } else {
            // Check if all items are from the same restaurant
            // If mixed, set cart restaurant to null to indicate multi-restaurant cart
            boolean hasMultipleRestaurants = cart.getItems().stream()
                    .anyMatch(item -> {
                        Restaurant itemRest = extractRestaurant(item.getMenuItem());
                        return itemRest == null || !Objects.equals(itemRest.getId(), itemRestaurant.getId());
                    });

            if (hasMultipleRestaurants) {
                // Mixed restaurants - clear cart restaurant to indicate multi-restaurant
                cart.setRestaurant(null);
                cart.setChefProfile(null);
            } else {
                // All items from same restaurant - keep it set for backward compatibility
                cart.setRestaurant(itemRestaurant);
                cart.setChefProfile(null);
            }
        }

        CartItem existingItem = findItemByMenuItem(cart, menuItemId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setLineTotal(calculateLineTotal(existingItem.getUnitPrice(), existingItem.getQuantity()));
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setMenuItem(menuItem);
            newItem.setQuantity(quantity);
            // Use effective price (after discount)
            newItem.setUnitPrice(menuItem.getEffectivePrice());
            newItem.setLineTotal(calculateLineTotal(newItem.getUnitPrice(), quantity));
            cart.getItems().add(newItem);
        }

        recalculateTotals(cart);
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(User user, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemById(cart, cartItemId);
        if (item == null) {
            throw new IllegalArgumentException("Cart item not found");
        }

        if (quantity < 1) {
            detachItem(item);
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
            item.setLineTotal(calculateLineTotal(item.getUnitPrice(), quantity));
        }

        recalculateTotals(cart);
        return cartRepository.save(cart);
    }

    public Cart removeItem(User user, Long cartItemId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = findItemById(cart, cartItemId);
        if (item == null) {
            throw new IllegalArgumentException("Cart item not found");
        }

        detachItem(item);
        cart.getItems().remove(item);
        recalculateTotals(cart);
        return cartRepository.save(cart);
    }

    public void clearCart(User user) {
        findCart(user).ifPresent(this::clearCart);
    }

    public void clearCart(Cart cart) {
        for (CartItem item : cart.getItems()) {
            detachItem(item);
        }
        cart.getItems().clear();
        cart.setRestaurant(null);
        cart.setChefProfile(null);
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public int getItemCount(User user) {
        return findCart(user)
                .map(c -> c.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    /**
     * Check if the cart contains items from multiple restaurants
     */
    @Transactional(readOnly = true)
    public boolean hasMultipleRestaurants(Cart cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            return false;
        }

        // If cart restaurant is null, it means mixed restaurants
        if (cart.getRestaurant() == null && cart.getItems().size() > 1) {
            return true;
        }

        // Check if items are from different restaurants
        Restaurant firstRestaurant = null;

        for (CartItem item : cart.getItems()) {
            Restaurant itemRest = extractRestaurant(item.getMenuItem());

            if (firstRestaurant == null) {
                firstRestaurant = itemRest;
            } else {
                if (itemRest == null || !Objects.equals(firstRestaurant.getId(), itemRest.getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    private CartItem findItemById(Cart cart, Long cartItemId) {
        if (cartItemId == null) {
            return null;
        }
        return cart.getItems().stream()
                .filter(item -> cartItemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    private CartItem findItemByMenuItem(Cart cart, Long menuItemId) {
        return cart.getItems().stream()
                .filter(item -> item.getMenuItem() != null && menuItemId.equals(item.getMenuItem().getId()))
                .findFirst()
                .orElse(null);
    }

    private void recalculateTotals(Cart cart) {
        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) {
            cart.setRestaurant(null);
            cart.setChefProfile(null);
            cart.setTotalAmount(BigDecimal.ZERO);
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal lineTotal = calculateLineTotal(item.getUnitPrice(), item.getQuantity());
            item.setLineTotal(lineTotal);
            total = total.add(lineTotal);
        }
        cart.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));

        // Update cart restaurant based on items
        // If all items are from same restaurant, set it; otherwise null
        if (items.size() > 0) {
            Restaurant firstRestaurant = extractRestaurant(items.get(0).getMenuItem());

            boolean allSameRestaurant = items.stream().allMatch(item -> {
                Restaurant itemRest = extractRestaurant(item.getMenuItem());
                return firstRestaurant != null && itemRest != null &&
                        Objects.equals(firstRestaurant.getId(), itemRest.getId());
            });

            if (allSameRestaurant && firstRestaurant != null) {
                cart.setRestaurant(firstRestaurant);
                cart.setChefProfile(null);
            } else {
                // Mixed restaurants - clear to indicate multi-restaurant cart
                cart.setRestaurant(null);
                cart.setChefProfile(null);
            }
        }
    }

    private void detachItem(CartItem item) {
        item.setCart(null);
        item.setMenuItem(null);
    }

    private BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private Restaurant extractRestaurant(MenuItem menuItem) {
        if (menuItem.getMenu() == null) {
            return null;
        }
        return menuItem.getMenu().getRestaurant();
    }

    // Chef feature removed - no longer extracting chef profiles
}
