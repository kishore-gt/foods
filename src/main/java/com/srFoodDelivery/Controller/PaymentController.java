package com.srFoodDelivery.Controller;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.PaymentSession;
import com.srFoodDelivery.model.RiderOffer;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.CartService;
import com.srFoodDelivery.service.EmailService;
import com.srFoodDelivery.service.OrderService;
import com.srFoodDelivery.service.order.OrderOrchestrationService;
import com.srFoodDelivery.service.rider.RiderService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/customer")
public class PaymentController {

    public static final String PAYMENT_SESSION_ATTRIBUTE = "PAYMENT_SESSION";

    private static final String OPTION_UPI = "UPI";
    private static final String OPTION_CARD = "CARD";
    private static final String OPTION_COD = "COD";

    private static final Pattern UPI_PATTERN = Pattern.compile("^[\\w.\\-]{2,}@[A-Za-z]{2,}$");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");
    private static final Pattern CARD_EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])\\/\\d{2}$");
    private static final Pattern CARD_CVV_PATTERN = Pattern.compile("^\\d{3,4}$");

    private final CartService cartService;
    private final OrderService orderService;
    private final OrderOrchestrationService orderOrchestrationService;
    private final MultiOrderRepository multiOrderRepository;
    private final RiderService riderService;
    private final EmailService emailService;

    public PaymentController(CartService cartService, OrderService orderService,
                             OrderOrchestrationService orderOrchestrationService,
                             MultiOrderRepository multiOrderRepository,
                             RiderService riderService,
                             EmailService emailService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderOrchestrationService = orderOrchestrationService;
        this.multiOrderRepository = multiOrderRepository;
        this.riderService = riderService;
        this.emailService = emailService;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return 0;
        }
        return cartService.getItemCount(principal.getUser());
    }

    @GetMapping("/payment")
    public String paymentPage(@AuthenticationPrincipal CustomUserDetails principal,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User user = requireUser(principal);

        var cart = cartService.findCart(user).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/customer/cart";
        }

        PaymentSession paymentSession = (PaymentSession) session.getAttribute(PAYMENT_SESSION_ATTRIBUTE);
        if (paymentSession == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Checkout session expired. Please try again.");
            return "redirect:/customer/cart";
        }

        if (!StringUtils.hasText(paymentSession.getSelectedPaymentOption())) {
            paymentSession.setSelectedPaymentOption(OPTION_UPI);
        }

        model.addAttribute("cart", cart);
        model.addAttribute("items", cart.getItems());
        model.addAttribute("paymentSession", paymentSession);
        return "customer/payment";
    }

    @PostMapping("/payment/confirm")
    public String confirmPayment(@AuthenticationPrincipal CustomUserDetails principal,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes,
                                 @RequestParam(name = "paymentOption", required = false) String paymentOption,
                                 @RequestParam(name = "upiId", required = false) String upiId,
                                 @RequestParam(name = "cardHolderName", required = false) String cardHolderName,
                                 @RequestParam(name = "cardNumber", required = false) String cardNumber,
                                 @RequestParam(name = "cardExpiry", required = false) String cardExpiry,
                                 @RequestParam(name = "cardCvv", required = false) String cardCvv) {
        User user = requireUser(principal);

        PaymentSession paymentSession = (PaymentSession) session.getAttribute(PAYMENT_SESSION_ATTRIBUTE);
        if (paymentSession == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Checkout session expired. Please try again.");
            return "redirect:/customer/cart";
        }

        String normalizedOption = paymentOption != null ? paymentOption.trim().toUpperCase(Locale.ENGLISH) : "";
        if (!StringUtils.hasText(normalizedOption)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a payment option.");
            return "redirect:/customer/payment";
        }
        paymentSession.setSelectedPaymentOption(normalizedOption);

        if (OPTION_COD.equals(normalizedOption)) {
            return finalizeOrder(session, redirectAttributes, paymentSession, user, "Cash on Delivery");
        }

        if (OPTION_UPI.equals(normalizedOption)) {
            String trimmedUpi = upiId != null ? upiId.trim() : "";
            paymentSession.setUpiId(trimmedUpi);
            if (!StringUtils.hasText(trimmedUpi) || !UPI_PATTERN.matcher(trimmedUpi).matches()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Enter a valid UPI ID (example: name@bank).");
                return "redirect:/customer/payment";
            }
            return finalizeOrder(session, redirectAttributes, paymentSession, user, "UPI");
        }

        if (OPTION_CARD.equals(normalizedOption)) {
            String holder = cardHolderName != null ? cardHolderName.trim() : "";
            String numberInput = cardNumber != null ? cardNumber.replaceAll("[\\s-]", "") : "";
            String expiryInput = cardExpiry != null ? cardExpiry.trim() : "";
            String cvvInput = cardCvv != null ? cardCvv.trim() : "";

            paymentSession.setCardHolderName(holder);
            paymentSession.setCardNumber(numberInput);
            paymentSession.setCardExpiry(expiryInput);

            String validationMessage = validateCard(holder, numberInput, expiryInput, cvvInput);
            if (validationMessage != null) {
                redirectAttributes.addFlashAttribute("errorMessage", validationMessage);
                return "redirect:/customer/payment";
            }

            paymentSession.setCardCvv(null);
            return finalizeOrder(session, redirectAttributes, paymentSession, user, "Card");
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Unsupported payment option selected.");
        return "redirect:/customer/payment";
    }

    @PostMapping("/payment/cancel")
    public String cancelPayment(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute(PAYMENT_SESSION_ATTRIBUTE);
        redirectAttributes.addFlashAttribute("infoMessage", "Payment cancelled.");
        return "redirect:/customer/cart";
    }

    private String finalizeOrder(HttpSession session,
                                 RedirectAttributes redirectAttributes,
                                 PaymentSession paymentSession,
                                 User user,
                                 String paymentDisplayName) {
        try {
            // Get reservation ID from session if available
            Long reservationId = null;
            Object reservationIdObj = session.getAttribute("preorderReservationId");
            if (reservationIdObj instanceof Long) {
                reservationId = (Long) reservationIdObj;
            } else if (reservationIdObj instanceof String) {
                try {
                    reservationId = Long.parseLong((String) reservationIdObj);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            
            orderService.placeOrder(user, paymentSession.getDeliveryAddress(), paymentSession.getSpecialInstructions(),
                    paymentSession.getDeliveryLocation(), paymentSession.getDiscountAmount(), paymentSession.getAppliedCoupon(),
                    paymentSession.getPreorderSlotId(), reservationId);
            
            // Update the latest order status to CONFIRMED after payment
            // Get the most recent order for this user and update its status
            var latestOrders = orderOrchestrationService.getUserMultiOrders(user);
            if (!latestOrders.isEmpty()) {
                var latestOrderDTO = latestOrders.get(0); // Most recent order
                // Use findByIdAndUser to eagerly fetch sub-orders
                var multiOrderOpt = multiOrderRepository.findByIdAndUser(latestOrderDTO.getId(), user);
                if (multiOrderOpt.isPresent()) {
                    var multiOrder = multiOrderOpt.get();
                    if ("PENDING".equals(multiOrder.getStatus())) {
                        // Check if it's a preorder
                        boolean isPreorder = "PREORDER".equals(multiOrder.getOrderingMode()) || 
                                           multiOrder.getSubOrders().stream()
                                               .anyMatch(so -> so.getPreorderSlot() != null);
                        
                        if (isPreorder) {
                            // Preorders need owner approval - set to PENDING_APPROVAL
                            multiOrder.setStatus("PENDING_APPROVAL");
                        } else {
                            // Delivery orders are confirmed immediately
                            multiOrder.setStatus("CONFIRMED");
                            
                            // Update all sub-orders to CONFIRMED (Ordered) status for delivery orders
                            for (var subOrder : multiOrder.getSubOrders()) {
                                // Only update delivery orders (not preorders)
                                if (subOrder.getPreorderSlot() == null && 
                                    subOrder.getReservation() == null && 
                                    subOrder.getTable() == null && 
                                    !"DINE_IN".equals(subOrder.getOrderType())) {
                                    if ("PENDING".equals(subOrder.getStatus())) {
                                        subOrder.setStatus("CONFIRMED");
                                    }
                                }
                            }
                        }
                        
                        // For COD, payment status remains PENDING; for others, set to PAID
                        if (!"Cash on Delivery".equals(paymentDisplayName)) {
                            multiOrder.setPaymentStatus("PAID");
                        }
                        
                        multiOrderRepository.save(multiOrder);
                        
                        // Send order confirmation email to customer
                        try {
                            emailService.sendMultiOrderConfirmationEmail(user, multiOrder);
                        } catch (Exception e) {
                            // Log error but don't fail the order
                            System.err.println("Failed to send confirmation email for MultiOrder " + multiOrder.getId() + ": " + e.getMessage());
                        }
                        
                        // Automatically broadcast orders to all riders for delivery orders
                        for (var subOrder : multiOrder.getSubOrders()) {
                            // Only broadcast delivery orders (not preorders)
                            if (subOrder.getPreorderSlot() == null && 
                                subOrder.getReservation() == null && 
                                subOrder.getTable() == null &&
                                !"DINE_IN".equals(subOrder.getOrderType()) &&
                                subOrder.getRider() == null) {
                                try {
                                    // Broadcast to ALL online riders (they can accept)
                                    List<RiderOffer> offers = riderService.broadcastOrderToAllRiders(subOrder.getId());
                                    System.out.println("Broadcasted SubOrder " + subOrder.getId() + " to " + offers.size() + " riders");
                                } catch (Exception e) {
                                    // Log error but don't fail the order
                                    // Rider assignment can be retried later
                                    System.err.println("Failed to broadcast order to riders for SubOrder " + subOrder.getId() + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order placed successfully using " + paymentDisplayName + "!");
            session.removeAttribute(PAYMENT_SESSION_ATTRIBUTE);
            return "redirect:/customer/orders";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/payment";
        }
    }

    private String validateCard(String holder, String number, String expiry, String cvv) {
        if (!StringUtils.hasText(holder)) {
            return "Card holder name is required.";
        }
        if (!CARD_NUMBER_PATTERN.matcher(number).matches()) {
            return "Enter a 16-digit card number without spaces.";
        }
        if (!CARD_EXPIRY_PATTERN.matcher(expiry).matches()) {
            return "Enter expiry in MM/YY format.";
        }
        if (isExpired(expiry)) {
            return "Card expiry must be in the future.";
        }
        if (!CARD_CVV_PATTERN.matcher(cvv).matches()) {
            return "Enter a valid 3 or 4 digit CVV.";
        }
        return null;
    }

    private boolean isExpired(String expiry) {
        if (!CARD_EXPIRY_PATTERN.matcher(expiry).matches()) {
            return true;
        }
        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = Integer.parseInt(expiry.substring(3, 5));
        YearMonth cardYearMonth = YearMonth.of(2000 + year, month);
        return cardYearMonth.isBefore(YearMonth.now());
    }

    private User requireUser(CustomUserDetails principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return principal.getUser();
    }
}
