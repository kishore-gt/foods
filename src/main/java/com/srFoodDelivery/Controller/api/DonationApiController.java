package com.srFoodDelivery.Controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.model.Donation;
import com.srFoodDelivery.service.DonationService;

@RestController
@RequestMapping("/api/donations")
public class DonationApiController {

    private final DonationService donationService;

    public DonationApiController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping
    public List<Donation> getAvailableDonations() {
        return donationService.getAvailableDonations();
    }
}

