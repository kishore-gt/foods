package com.srFoodDelivery.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.srFoodDelivery.dto.RegistrationDto;
import com.srFoodDelivery.model.UserRole;
import com.srFoodDelivery.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registration")) {
            model.addAttribute("registration", new RegistrationDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute("registration") RegistrationDto registration,
                                     BindingResult bindingResult,
                                     Model model) {
        if (!registration.getPassword().equals(registration.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "registration.password.mismatch", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(registration);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "registration.email.exists", ex.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }

    @ModelAttribute("roles")
    public String[] roles() {
        return new String[] { UserRole.CUSTOMER, UserRole.OWNER, UserRole.RIDER, UserRole.COMPANY };
    }

    @ModelAttribute("roleLabels")
    public Map<String, String> roleLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(UserRole.CUSTOMER, "Customer");
        labels.put(UserRole.OWNER, "Restaurant Owner");
        labels.put(UserRole.RIDER, "Rider");
        labels.put(UserRole.COMPANY, "Company");
        return labels;
    }
}
