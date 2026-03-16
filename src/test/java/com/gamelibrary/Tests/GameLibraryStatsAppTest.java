package com.gamelibrary.Tests;

import com.gamelibrary.stats.GameLibraryStatsApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Covers the Spring Boot application class to lift overall coverage.
 * We start and then immediately close the context using the "test" profile.
 */
public class GameLibraryStatsAppTest {

    @Test
    @DisplayName("main() boots the Spring context (servlet mode) and closes without error")
    void main_starts_and_closes_context() {
        SpringApplication app = new SpringApplication(GameLibraryStatsApp.class);
        ConfigurableApplicationContext ctx = app.run("--spring.profiles.active=test", "--spring.main.banner-mode=off");
        // If the app fails to start, an exception would be thrown before this line
        ctx.close();
    }
}
