package com.gamelibrary.Tests.karate;

import com.gamelibrary.stats.GameLibraryStatsApp;
import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = GameLibraryStatsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthKarateRunnerTest {

    @LocalServerPort
    int port;

    @BeforeAll
    static void beforeAll() {
        // nothing global yet
        assertTrue(true);
    }

    @Karate.Test
    public Karate runAuth() {
        System.setProperty("baseUrl", "http://localhost:" + port);
        return Karate.run("classpath:karate/auth/auth.feature");
    }
}
