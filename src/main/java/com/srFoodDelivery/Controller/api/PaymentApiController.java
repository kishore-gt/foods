package com.srFoodDelivery.Controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.dto.payment.DemoPaymentRequest;
import com.srFoodDelivery.dto.payment.DemoPaymentResponse;
import com.srFoodDelivery.dto.payment.OtpVerificationRequest;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.payment.DemoPaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {

    private final DemoPaymentService demoPaymentService;

    public PaymentApiController(DemoPaymentService demoPaymentService) {
        this.demoPaymentService = demoPaymentService;
    }

    @PostMapping("/demo")
    public ResponseEntity<DemoPaymentResponse> processDemoPayment(
            @Valid @RequestBody DemoPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        DemoPaymentResponse response = demoPaymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/demo/verify-otp")
    public ResponseEntity<DemoPaymentResponse> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        DemoPaymentResponse response = demoPaymentService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}

