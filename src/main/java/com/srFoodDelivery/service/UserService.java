package com.srFoodDelivery.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.RegistrationDto;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole().toUpperCase());
        user.setPhoneNumber(dto.getPhoneNumber());

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    public User getByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByRole(String role) {
        return userRepository.findByRole(role.toUpperCase());
    }

    public long countByRole(String role) {
        return userRepository.countByRole(role.toUpperCase());
    }

    @Transactional
    public User updateUser(Long id, String fullName, String email, String phoneNumber, String role, String deliveryLocation) {
        User user = getById(id);
        
        // Check if email is being changed and if it's already in use
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        
        user.setFullName(fullName);
        user.setEmail(email.toLowerCase());
        user.setPhoneNumber(phoneNumber);
        user.setRole(role.toUpperCase());
        if (deliveryLocation != null) {
            user.setDeliveryLocation(deliveryLocation);
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserPassword(Long id, String newPassword) {
        User user = getById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
