package com.srFoodDelivery.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.payment.DemoPaymentRequest;
import com.srFoodDelivery.dto.payment.DemoPaymentResponse;
import com.srFoodDelivery.dto.payment.OtpVerificationRequest;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Payment;
import com.srFoodDelivery.model.RiderOffer;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.PaymentRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.service.rider.RiderService;

@Service
@Transactional
public class DemoPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(DemoPaymentService.class);
    private static final BigDecimal OTP_THRESHOLD = new BigDecimal("500.00");
    private static final String VALID_OTP = "123456"; // Demo OTP
    private static final int OTP_EXPIRY_MINUTES = 10;

    private final PaymentRepository paymentRepository;
    private final MultiOrderRepository multiOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final RiderService riderService;

    public DemoPaymentService(
            PaymentRepository paymentRepository,
            MultiOrderRepository multiOrderRepository,
            SubOrderRepository subOrderRepository,
            RiderService riderService) {
        this.paymentRepository = paymentRepository;
        this.multiOrderRepository = multiOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.riderService = riderService;
    }

    /**
     * Processes a demo payment. If amount < 500, auto-approves.
     * If amount >= 500, requires OTP verification.
     */
    public DemoPaymentResponse processPayment(DemoPaymentRequest request) {
        logger.info("Processing demo payment for MultiOrder: {}, Amount: {}", 
                request.getMultiOrderId(), request.getAmount());

        MultiOrder multiOrder = multiOrderRepository.findById(request.getMultiOrderId())
                .orElseThrow(() -> new IllegalArgumentException("MultiOrder not found"));

        if (!"PENDING".equals(multiOrder.getPaymentStatus())) {
            throw new IllegalStateException("MultiOrder payment status is not PENDING");
        }

        if (request.getAmount().compareTo(multiOrder.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match order total");
        }

        Payment payment = new Payment();
        payment.setMultiOrder(multiOrder);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod("DEMO");
        payment.setTransactionId(UUID.randomUUID().toString());

        DemoPaymentResponse response = new DemoPaymentResponse();
        response.setAmount(request.getAmount());

        // If amount < 500, auto-approve
        if (request.getAmount().compareTo(OTP_THRESHOLD) < 0) {
            payment.setPaymentStatus("PAID");
            payment.setOtpRequired(false);
            payment.setOtpVerified(true);
            
            Payment savedPayment = paymentRepository.save(payment);
            
            // Update MultiOrder payment status
            multiOrder.setPaymentStatus("PAID");
            multiOrder.setStatus("CONFIRMED");
            multiOrderRepository.save(multiOrder);
            
            // Dispatch suborders
            dispatchSubOrders(multiOrder);
            
            response.setPaymentId(savedPayment.getId());
            response.setPaymentStatus("PAID");
            response.setRequiresOtp(false);
            response.setMessage("Payment successful");
            
            logger.info("Payment auto-approved for MultiOrder: {}", multiOrder.getId());
        } else {
            // Amount >= 500, require OTP
            String otpCode = generateOtp();
            payment.setPaymentStatus("PENDING");
            payment.setOtpRequired(true);
            payment.setOtpCode(otpCode);
            payment.setOtpVerified(false);
            payment.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            
            Payment savedPayment = paymentRepository.save(payment);
            
            response.setPaymentId(savedPayment.getId());
            response.setPaymentStatus("PENDING");
            response.setRequiresOtp(true);
            response.setMessage("OTP required. Use OTP: " + otpCode + " (demo mode)");
            
            logger.info("Payment requires OTP for MultiOrder: {}, OTP: {}", multiOrder.getId(), otpCode);
        }

        return response;
    }

    /**
     * Verifies OTP and completes payment if valid.
     */
    public DemoPaymentResponse verifyOtp(OtpVerificationRequest request) {
        logger.info("Verifying OTP for payment: {}", request.getPaymentId());

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!payment.getOtpRequired()) {
            throw new IllegalStateException("This payment does not require OTP");
        }

        if (payment.getOtpVerified()) {
            throw new IllegalStateException("OTP already verified");
        }

        if (payment.getOtpExpiresAt() != null && 
            payment.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP has expired");
        }

        // Verify OTP (demo: accept "123456")
        if (!VALID_OTP.equals(request.getOtp())) {
            DemoPaymentResponse response = new DemoPaymentResponse();
            response.setPaymentId(payment.getId());
            response.setPaymentStatus("PENDING");
            response.setRequiresOtp(true);
            response.setMessage("Invalid OTP");
            return response;
        }

        // OTP verified
        payment.setOtpVerified(true);
        payment.setPaymentStatus("PAID");
        paymentRepository.save(payment);

        MultiOrder multiOrder = payment.getMultiOrder();
        multiOrder.setPaymentStatus("PAID");
        multiOrder.setStatus("CONFIRMED");
        multiOrderRepository.save(multiOrder);

        // Dispatch suborders
        dispatchSubOrders(multiOrder);

        DemoPaymentResponse response = new DemoPaymentResponse();
        response.setPaymentId(payment.getId());
        response.setPaymentStatus("PAID");
        response.setRequiresOtp(false);
        response.setMessage("Payment successful");
        response.setAmount(payment.getAmount());

        logger.info("OTP verified and payment completed for MultiOrder: {}", multiOrder.getId());

        return response;
    }

    private void dispatchSubOrders(MultiOrder multiOrder) {
        logger.info("Dispatching SubOrders for MultiOrder: {}", multiOrder.getId());
        
        for (SubOrder subOrder : multiOrder.getSubOrders()) {
            // Only dispatch delivery orders (not preorders or dine-in)
            if (subOrder.getPreorderSlot() != null || 
                subOrder.getReservation() != null || 
                subOrder.getTable() != null ||
                "DINE_IN".equals(subOrder.getOrderType()) ||
                "PREORDER".equals(subOrder.getOrderType())) {
                logger.info("Skipping dispatch for SubOrder {} (preorder/dine-in)", subOrder.getId());
                continue;
            }
            
            subOrder.setStatus("CONFIRMED");
            subOrderRepository.save(subOrder);
            
            // Broadcast order to ALL online riders (NEW: All riders can see and accept)
            if (subOrder.getRestaurant() != null) {
                try {
                    List<RiderOffer> offers = riderService.broadcastOrderToAllRiders(subOrder.getId());
                    logger.info("Broadcasted SubOrder {} to {} riders for restaurant {}", 
                            subOrder.getId(), offers.size(), subOrder.getRestaurant().getId());
                } catch (Exception e) {
                    logger.warn("Failed to broadcast order to riders for SubOrder {}: {}", 
                            subOrder.getId(), e.getMessage());
                }
            }
        }
    }

    private String generateOtp() {
        // In demo mode, always return the valid OTP
        // In production, this would generate a random 6-digit code
        return VALID_OTP;
    }
}

