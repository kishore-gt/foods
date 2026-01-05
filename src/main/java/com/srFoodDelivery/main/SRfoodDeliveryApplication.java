package com.srFoodDelivery.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.srFoodDelivery")
@EntityScan(basePackages = "com.srFoodDelivery.model")
@EnableJpaRepositories(basePackages = "com.srFoodDelivery.repository")
@EnableScheduling
public class SRfoodDeliveryApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SRfoodDeliveryApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SRfoodDeliveryApplication.class, args);
    }
}
