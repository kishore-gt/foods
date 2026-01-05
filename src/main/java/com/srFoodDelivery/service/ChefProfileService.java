package com.srFoodDelivery.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.ChefProfileRepository;

@Service
public class ChefProfileService {

    private final ChefProfileRepository chefProfileRepository;

    public ChefProfileService(ChefProfileRepository chefProfileRepository) {
        this.chefProfileRepository = chefProfileRepository;
    }

    @Transactional
    public ChefProfile getOrCreateProfile(User chef) {
        return chefProfileRepository.findByChef(chef).orElseGet(() -> {
            ChefProfile profile = new ChefProfile();
            profile.setChef(chef);
            return chefProfileRepository.save(profile);
        });
    }

    @Transactional
    public ChefProfile createProfile(User chef) {
        ChefProfile profile = new ChefProfile();
        profile.setChef(chef);
        return chefProfileRepository.save(profile);
    }

    @Transactional
    public ChefProfile updateProfile(User chef, String bio, String speciality, String location) {
        ChefProfile profile = getOrCreateProfile(chef);
        profile.setBio(bio);
        profile.setSpeciality(speciality);
        profile.setLocation(location);
        return profile;
    }
}
