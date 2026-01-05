package com.srFoodDelivery.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    @Primary
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            logger.info("Starting Flyway migration with automatic failed migration cleanup...");
            
            // Clean up failed migrations before migrating
            try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
                // Check if flyway_schema_history table exists
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'")) {
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Table exists, clean up failed migrations
                            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM flyway_schema_history WHERE success = 0")) {
                                int deleted = stmt.executeUpdate();
                                if (deleted > 0) {
                                    logger.info("✓ Cleaned up {} failed migration record(s)", deleted);
                                } else {
                                    logger.debug("No failed migrations found to clean up");
                                }
                            }
                        } else {
                            logger.debug("flyway_schema_history table does not exist yet (first run)");
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not clean failed migrations: {}", e.getMessage());
                // Continue anyway - might be first run or table doesn't exist
            }
            
            // Repair checksums
            try {
                flyway.repair();
                logger.debug("Flyway repair completed successfully");
            } catch (Exception e) {
                logger.warn("Flyway repair failed (this is OK if no checksum issues): {}", e.getMessage());
            }
            
            // Now run migrations
            try {
                flyway.migrate();
                logger.info("Flyway migration completed successfully");
            } catch (Exception e) {
                logger.error("Flyway migration failed: {}", e.getMessage(), e);
                throw e; // Re-throw to prevent app from starting with broken schema
            }
        };
    }
}

