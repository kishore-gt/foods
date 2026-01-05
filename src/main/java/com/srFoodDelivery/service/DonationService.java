package com.srFoodDelivery.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.DonationForm;
import com.srFoodDelivery.model.Donation;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.DonationRepository;

@Service
@Transactional
public class DonationService {

    private final DonationRepository donationRepository;

    public DonationService(DonationRepository donationRepository) {
        this.donationRepository = donationRepository;
    }

    public Donation createDonation(Restaurant restaurant, DonationForm form) {
        // Check if restaurant is closed (after closing time)
        if (!isAfterClosingTime(restaurant)) {
            throw new IllegalStateException("Donations can only be made after restaurant closing time");
        }

        Donation donation = new Donation();
        donation.setRestaurant(restaurant);
        donation.setFoodName(form.getFoodName());
        donation.setQuantity(form.getQuantity());
        donation.setDescription(form.getDescription());
        donation.setInGoodCondition(form.getInGoodCondition());
        donation.setIsClaimed(false);

        return donationRepository.save(donation);
    }

    public Donation claimDonation(Long donationId, User user) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found"));

        if (donation.getIsClaimed()) {
            throw new IllegalStateException("This donation has already been claimed");
        }

        donation.setIsClaimed(true);
        donation.setClaimedBy(user);
        donation.setClaimedAt(LocalDateTime.now());

        return donationRepository.save(donation);
    }

    @Transactional(readOnly = true)
    public List<Donation> getRestaurantDonations(Restaurant restaurant) {
        return donationRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    @Transactional(readOnly = true)
    public List<Donation> getAvailableDonations() {
        return donationRepository.findByIsClaimedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Donation> getUserClaimedDonations(User user) {
        return donationRepository.findByClaimedByOrderByClaimedAtDesc(user);
    }

    private boolean isAfterClosingTime(Restaurant restaurant) {
        if (restaurant.getClosingTime() == null || restaurant.getClosingTime().isEmpty()) {
            return true; // If no closing time set, allow donations
        }

        try {
            LocalTime closingTime = LocalTime.parse(restaurant.getClosingTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime now = LocalTime.now();
            return now.isAfter(closingTime);
        } catch (Exception e) {
            return true; // If parsing fails, allow donations
        }
    }
}

