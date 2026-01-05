package com.srFoodDelivery.main;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.UserRepository;

@Component
public class UserDiagnosticRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserDiagnosticRunner(UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String email = "Gtechno1@gmail.com";
        System.out.println("====== USER DIAGNOSTIC START ======");
        userRepository.findByEmail(email.toLowerCase()).ifPresentOrElse(
                user -> {
                    System.out.println("User found: " + user.getEmail());
                    System.out.println("Current Role: " + user.getRole());

                    // Update role if needed
                    if (!"COMPANY".equals(user.getRole())) {
                        System.out.println("Updating role to COMPANY");
                        user.setRole("COMPANY");
                        userRepository.save(user);
                    }

                    // Reset password to 123456
                    System.out.println("Resetting password to 123456");
                    user.setPasswordHash(passwordEncoder.encode("123456"));
                    userRepository.save(user);
                },
                () -> {
                    System.out.println("User NOT found. Creating new COMPANY user.");
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName("Gtechno Company");
                    newUser.setRole("COMPANY");
                    newUser.setPasswordHash(passwordEncoder.encode("123456"));
                    newUser.setPhoneNumber("1234567890");
                    newUser.setDeliveryLocation("Tech Park");
                    userRepository.save(newUser);
                    System.out.println("User created successfully.");
                });
        System.out.println("====== USER DIAGNOSTIC END ======");
    }
}
