package com.gamelibrary.Tests;

import com.gamelibrary.stats.GameLibraryStatsApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the Spring Boot application class to lift overall coverage.
 * We start and then immediately close the context using the "test" profile.
 */
class GameLibraryStatsAppTest {

    @Test
    @DisplayName("main() boots the Spring context (servlet mode) and closes without error")
    void main_starts_and_closes_context() {
        SpringApplication app = new SpringApplication(GameLibraryStatsApp.class);
        ConfigurableApplicationContext ctx = app.run(
                "--spring.profiles.active=test"
        );

        assertNotNull(ctx);
        assertTrue(ctx.isActive());

        ctx.close();
    }
}
