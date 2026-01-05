package com.srFoodDelivery.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.CompanySubscription;
import com.srFoodDelivery.repository.CompanySubscriptionRepository;

@Component
public class SubscriptionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final CompanyService companyService;

    public SubscriptionScheduler(
            CompanySubscriptionRepository companySubscriptionRepository,
            CompanyService companyService) {
        this.companySubscriptionRepository = companySubscriptionRepository;
        this.companyService = companyService;
    }

    /**
     * Runs every day at 6:00 AM to generate daily orders for active subscriptions.
     * Cron expression: second, minute, hour, day of month, month, day(s) of week
     */
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void generateDailySubscriptionOrders() {
        LocalDate today = LocalDate.now();
        logger.info("Starting daily subscription order generation for date: {}", today);

        // Fetch all active subscriptions where end date is today or later
        List<CompanySubscription> activeSubscriptions = companySubscriptionRepository
                .findByStatusAndEndDateAfter("ACTIVE", today.minusDays(1));

        logger.info("Found {} potentially active subscriptions", activeSubscriptions.size());

        for (CompanySubscription subscription : activeSubscriptions) {
            try {
                if (shouldSkipToday(subscription, today)) {
                    logger.info("Skipping subscription {} for company {} (Day excluded)",
                            subscription.getId(), subscription.getCompany().getCompanyName());
                    continue;
                }

                logger.info("Generating order for subscription {} - Company: {}",
                        subscription.getId(), subscription.getCompany().getCompanyName());

                companyService.placeDailyOrder(
                        subscription.getCompany(),
                        today,
                        null, // Use default address or previous logic inside service
                        "Auto-generated daily subscription order");

                logger.info("Successfully generated daily order for subscription {}", subscription.getId());

            } catch (Exception e) {
                logger.error("Failed to generate order for subscription {}: {}",
                        subscription.getId(), e.getMessage(), e);
            }
        }

        logger.info("Completed daily subscription order generation");
    }

    private boolean shouldSkipToday(CompanySubscription subscription, LocalDate date) {
        String excludedDays = subscription.getExcludedDays();
        if (excludedDays == null || excludedDays.trim().isEmpty()) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // Check if today matches any excluded day (case-insensitive)
        for (String excluded : excludedDays.split(",")) {
            if (excluded.trim().equalsIgnoreCase(dayName)) {
                return true;
            }
        }

        return false;
    }
}
