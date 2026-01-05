package com.srFoodDelivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.srFoodDelivery.model.UserRole;
import com.srFoodDelivery.security.CustomUserDetailsService;
import com.srFoodDelivery.security.RoleBasedAuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()
                // Public pages - root must be explicitly first
                .requestMatchers("/").permitAll()
                .requestMatchers("/login", "/register", "/register/**", "/about.html", "/blog.html", "/testimonial.html").permitAll()
                .requestMatchers("/api/**").permitAll()
                // Allow guests to view restaurants, menus, categories, and offers
                .requestMatchers("/customer/restaurants").permitAll()
                .requestMatchers("/customer/restaurants/*").permitAll()
                .requestMatchers("/customer/menus/*").permitAll()
                .requestMatchers("/customer/offers").permitAll()
                .requestMatchers("/customer/category/*").permitAll()
                // Require authentication for cart, orders, and other customer actions
                .requestMatchers("/customer/cart/**", "/customer/orders/**", 
                                 "/customer/payment/**", "/customer/*/review/**", "/customer/review/**").hasRole(UserRole.CUSTOMER)
                .requestMatchers("/admin/**").hasRole(UserRole.ADMIN)
                .requestMatchers("/owner/**").hasAnyRole(UserRole.OWNER, UserRole.CAFE_OWNER)
                .requestMatchers("/chef/**").hasRole(UserRole.CHEF)
                .requestMatchers("/rider/**").hasRole(UserRole.RIDER)
                .requestMatchers("/company/**").hasRole(UserRole.COMPANY)
                .anyRequest().authenticated())
            .formLogin(login -> login
                .loginPage("/login")
                .successHandler(new RoleBasedAuthenticationSuccessHandler())
                .permitAll()
                .failureUrl("/login?error=true"))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true));

        return http.build();
    }
}
