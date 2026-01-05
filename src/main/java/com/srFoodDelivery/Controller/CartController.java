package com.srFoodDelivery.Controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srFoodDelivery.model.Cart;
import com.srFoodDelivery.model.PaymentSession;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.UserRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.model.SiteMode;
import com.srFoodDelivery.service.CartService;
import com.srFoodDelivery.service.OfferService;
import com.srFoodDelivery.service.OrderService;
import com.srFoodDelivery.service.SiteModeManager;
import com.srFoodDelivery.service.order.OrderOrchestrationService;
import com.srFoodDelivery.repository.MenuItemRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/customer")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final OfferService offerService;
    private final UserRepository userRepository;
    private final OrderOrchestrationService orderOrchestrationService;
    private final SiteModeManager siteModeManager;

    public CartController(CartService cartService, OrderService orderService, OfferService offerService,
            UserRepository userRepository, OrderOrchestrationService orderOrchestrationService,
            SiteModeManager siteModeManager) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.offerService = offerService;
        this.userRepository = userRepository;
        this.orderOrchestrationService = orderOrchestrationService;
        this.siteModeManager = siteModeManager;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return 0;
        }
        return cartService.getItemCount(principal.getUser());
    }

    @GetMapping("/cart")
    public String viewCart(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        Cart cart = cartService.findCart(user)
                .orElseGet(() -> cartService.getOrCreateCart(user));

        // Group items by restaurant/chef for better display
        Map<String, List<com.srFoodDelivery.model.CartItem>> groupedItems = new LinkedHashMap<>();
        for (com.srFoodDelivery.model.CartItem item : cart.getItems()) {
            String groupKey = "unknown";
            if (item.getMenuItem() != null && item.getMenuItem().getMenu() != null) {
                if (item.getMenuItem().getMenu().getRestaurant() != null) {
                    groupKey = "restaurant_" + item.getMenuItem().getMenu().getRestaurant().getId();
                } else if (item.getMenuItem().getMenu().getChefProfile() != null) {
                    groupKey = "chef_" + item.getMenuItem().getMenu().getChefProfile().getId();
                }
            }
            groupedItems.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(item);
        }

        model.addAttribute("cart", cart);
        model.addAttribute("items", cart.getItems());
        model.addAttribute("groupedItems", groupedItems);
        model.addAttribute("hasMultipleRestaurants", cartService.hasMultipleRestaurants(cart));
        model.addAttribute("offers", offerService.getAvailableOffers(user, cart));
        model.addAttribute("user", user);
        return "customer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("menuItemId") Long menuItemId,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
            @RequestParam(name = "returnUrl", required = false) String returnUrl,
            @RequestParam(name = "returnQuery", required = false) String returnQuery,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        try {
            cartService.addItem(user, menuItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to cart.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + buildRedirect(returnUrl, returnQuery, request);
    }

    @PostMapping("/cart/update")
    public String updateCartItem(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("cartItemId") Long cartItemId,
            @RequestParam("quantity") int quantity,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        try {
            cartService.updateItemQuantity(user, cartItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cart updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/cart";
    }

    @PostMapping("/cart/remove")
    public String removeCartItem(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("cartItemId") Long cartItemId,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        try {
            cartService.removeItem(user, cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(@AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        cartService.clearCart(user);
        redirectAttributes.addFlashAttribute("successMessage", "Cart cleared.");
        return "redirect:/customer/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(@AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("deliveryAddress") String deliveryAddress,
            @RequestParam(name = "deliveryLocation", required = false) String deliveryLocation,
            @RequestParam(name = "specialInstructions", required = false) String specialInstructions,
            @RequestParam(name = "couponCode", required = false) String couponCode,
            @RequestParam(name = "preorderSlotId", required = false) Long preorderSlotId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        if (!StringUtils.hasText(deliveryAddress)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a delivery address.");
            return "redirect:/customer/cart";
        }

        Cart cart = cartService.findCart(user).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/customer/cart";
        }

        // Save delivery location to user
        if (StringUtils.hasText(deliveryLocation)) {
            user.setDeliveryLocation(deliveryLocation.trim());
            userRepository.save(user);
        }

        // Calculate discount
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCoupon = null;
        if (StringUtils.hasText(couponCode)) {
            discount = offerService.applyDiscount(couponCode, user, cart);
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                appliedCoupon = couponCode;
            }
        }

        // Get preorder slot ID from request parameter (passed from cart page) or
        // session
        if (preorderSlotId == null) {
            Object slotIdObj = session.getAttribute("preorderSlotId");
            if (slotIdObj instanceof Long) {
                preorderSlotId = (Long) slotIdObj;
            } else if (slotIdObj instanceof String) {
                try {
                    preorderSlotId = Long.parseLong((String) slotIdObj);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        PaymentSession paymentSession = new PaymentSession(deliveryAddress.trim(),
                StringUtils.hasText(specialInstructions) ? specialInstructions.trim() : null,
                deliveryLocation != null ? deliveryLocation.trim() : null,
                discount,
                appliedCoupon,
                preorderSlotId);
        session.setAttribute(PaymentController.PAYMENT_SESSION_ATTRIBUTE, paymentSession);

        // Reservation ID is already in session from preorder flow, no need to set it
        // again

        return "redirect:/customer/payment";
    }

    @GetMapping("/orders")
    public String orderHistory(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = requireUser(principal);
        // Get multi-orders (new system)
        model.addAttribute("multiOrders", orderOrchestrationService.getUserMultiOrders(user));
        return "customer/orders";
    }

    @GetMapping("/orders/{orderId}/track")
    public String trackMultiOrder(@PathVariable Long orderId,
            @RequestParam(required = false, name = "mode") String mode,
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);
        try {
            // Try to get as MultiOrder first
            var multiOrderDTO = orderOrchestrationService.getMultiOrderById(orderId, user);

            // Check if this is a preorder/dine-in order - if so, redirect to orders page
            boolean isPreorder = false;
            if (multiOrderDTO.getSubOrders() != null && !multiOrderDTO.getSubOrders().isEmpty()) {
                for (var subOrder : multiOrderDTO.getSubOrders()) {
                    if (subOrder.getReservationId() != null ||
                            subOrder.getTableId() != null ||
                            "DINE_IN".equals(subOrder.getOrderType())) {
                        isPreorder = true;
                        break;
                    }
                }
            }

            if (isPreorder) {
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Tracking is not available for dine-in/preorder orders. Please visit the restaurant at your reserved time.");
                return "redirect:/customer/orders";
            }

            // For delivery orders, show tracking
            SiteMode siteMode = siteModeManager.resolveMode(mode, session);
            model.addAttribute("multiOrder", multiOrderDTO);
            model.addAttribute("isMultiOrder", true);
            model.addAttribute("siteMode", siteMode);
            return "customer/track-order";
        } catch (IllegalArgumentException e) {
            // Fall back to legacy order
            var order = orderService.getOrder(orderId);
            if (!order.getUser().getId().equals(user.getId())) {
                throw new SecurityException("Access denied");
            }
            // Legacy orders don't have preorder support, so allow tracking
            SiteMode siteMode = siteModeManager.resolveMode(mode, session);
            model.addAttribute("order", order);
            model.addAttribute("isMultiOrder", false);
            model.addAttribute("siteMode", siteMode);
            return "customer/track-order";
        }
    }

    private User requireUser(CustomUserDetails principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return principal.getUser();
    }

    private String buildRedirect(String returnUrl, String returnQuery, HttpServletRequest request) {
        String path = null;
        String query = StringUtils.hasText(returnQuery) ? returnQuery : null;

        if (StringUtils.hasText(returnUrl)) {
            String trimmed = returnUrl.trim();
            int queryIndex = trimmed.indexOf('?');
            if (queryIndex >= 0) {
                path = trimmed.substring(0, queryIndex);
                if (!StringUtils.hasText(query) && queryIndex < trimmed.length() - 1) {
                    query = trimmed.substring(queryIndex + 1);
                }
            } else {
                path = trimmed;
            }
        }

        if (request != null) {
            String referer = request.getHeader("Referer");
            if (StringUtils.hasText(referer)) {
                try {
                    java.net.URI refererUri = java.net.URI.create(referer);
                    if (!StringUtils.hasText(path)) {
                        path = refererUri.getPath();
                    }
                    if (!StringUtils.hasText(query)) {
                        query = refererUri.getQuery();
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed referer headers and fall back to default path.
                }
            }
        }

        path = resolvePath(path);

        if (StringUtils.hasText(query)) {
            return path + (query.startsWith("?") ? query : "?" + query);
        }
        return path;
    }

    private String resolvePath(String candidate) {
        if (StringUtils.hasText(candidate) && candidate.startsWith("/")) {
            return candidate;
        }
        return "/customer/restaurants";
    }
}
