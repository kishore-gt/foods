package com.srFoodDelivery.service.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.payment.DemoPaymentRequest;
import com.srFoodDelivery.dto.payment.DemoPaymentResponse;
import com.srFoodDelivery.dto.payment.OtpVerificationRequest;
import com.srFoodDelivery.main.SRfoodDeliveryApplication;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.UserRepository;

@SpringBootTest(classes = SRfoodDeliveryApplication.class)
@ActiveProfiles("test")
@Transactional
public class DemoPaymentServiceTest {

    @Autowired
    private DemoPaymentService demoPaymentService;

    @Autowired
    private MultiOrderRepository multiOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testPaymentBelowThreshold_AutoApproved() {
        User user = userRepository.findByEmail("customer1@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        // Create a test multiorder
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setUser(user);
        multiOrder.setTotalAmount(new BigDecimal("100.00"));
        multiOrder.setDeliveryAddress("123 Test St");
        multiOrder.setStatus("PENDING");
        multiOrder.setPaymentStatus("PENDING");
        MultiOrder savedOrder = multiOrderRepository.save(multiOrder);

        DemoPaymentRequest request = new DemoPaymentRequest();
        request.setMultiOrderId(savedOrder.getId());
        request.setAmount(new BigDecimal("100.00"));

        DemoPaymentResponse response = demoPaymentService.processPayment(request);

        assertEquals("PAID", response.getPaymentStatus());
        assertFalse(response.getRequiresOtp());
        assertEquals("Payment successful", response.getMessage());

        // Verify order status updated
        MultiOrder updatedOrder = multiOrderRepository.findById(savedOrder.getId())
                .orElseThrow();
        assertEquals("PAID", updatedOrder.getPaymentStatus());
        assertEquals("CONFIRMED", updatedOrder.getStatus());
    }

    @Test
    public void testPaymentAboveThreshold_RequiresOtp() {
        User user = userRepository.findByEmail("customer1@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setUser(user);
        multiOrder.setTotalAmount(new BigDecimal("600.00"));
        multiOrder.setDeliveryAddress("123 Test St");
        multiOrder.setStatus("PENDING");
        multiOrder.setPaymentStatus("PENDING");
        MultiOrder savedOrder = multiOrderRepository.save(multiOrder);

        DemoPaymentRequest request = new DemoPaymentRequest();
        request.setMultiOrderId(savedOrder.getId());
        request.setAmount(new BigDecimal("600.00"));

        DemoPaymentResponse response = demoPaymentService.processPayment(request);

        assertEquals("PENDING", response.getPaymentStatus());
        assertTrue(response.getRequiresOtp());
        assertNotNull(response.getPaymentId());
    }

    @Test
    public void testOtpVerification_Success() {
        User user = userRepository.findByEmail("customer1@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setUser(user);
        multiOrder.setTotalAmount(new BigDecimal("600.00"));
        multiOrder.setDeliveryAddress("123 Test St");
        multiOrder.setStatus("PENDING");
        multiOrder.setPaymentStatus("PENDING");
        MultiOrder savedOrder = multiOrderRepository.save(multiOrder);

        // First, create payment requiring OTP
        DemoPaymentRequest paymentRequest = new DemoPaymentRequest();
        paymentRequest.setMultiOrderId(savedOrder.getId());
        paymentRequest.setAmount(new BigDecimal("600.00"));
        DemoPaymentResponse paymentResponse = demoPaymentService.processPayment(paymentRequest);

        // Then verify OTP
        OtpVerificationRequest otpRequest = new OtpVerificationRequest();
        otpRequest.setPaymentId(paymentResponse.getPaymentId());
        otpRequest.setOtp("123456"); // Valid demo OTP

        DemoPaymentResponse otpResponse = demoPaymentService.verifyOtp(otpRequest);

        assertEquals("PAID", otpResponse.getPaymentStatus());
        assertFalse(otpResponse.getRequiresOtp());
    }

    @Test
    public void testOtpVerification_InvalidOtp() {
        User user = userRepository.findByEmail("customer1@example.com")
                .orElseThrow(() -> new RuntimeException("Test user not found"));

        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setUser(user);
        multiOrder.setTotalAmount(new BigDecimal("600.00"));
        multiOrder.setDeliveryAddress("123 Test St");
        multiOrder.setStatus("PENDING");
        multiOrder.setPaymentStatus("PENDING");
        MultiOrder savedOrder = multiOrderRepository.save(multiOrder);

        DemoPaymentRequest paymentRequest = new DemoPaymentRequest();
        paymentRequest.setMultiOrderId(savedOrder.getId());
        paymentRequest.setAmount(new BigDecimal("600.00"));
        DemoPaymentResponse paymentResponse = demoPaymentService.processPayment(paymentRequest);

        OtpVerificationRequest otpRequest = new OtpVerificationRequest();
        otpRequest.setPaymentId(paymentResponse.getPaymentId());
        otpRequest.setOtp("000000"); // Invalid OTP

        DemoPaymentResponse otpResponse = demoPaymentService.verifyOtp(otpRequest);

        assertEquals("PENDING", otpResponse.getPaymentStatus());
        assertTrue(otpResponse.getRequiresOtp());
        assertEquals("Invalid OTP", otpResponse.getMessage());
    }
}

