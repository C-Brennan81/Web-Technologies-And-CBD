package com.gamelibrary.Tests.karate;

import com.intuit.karate.junit5.Karate;
import com.gamelibrary.stats.GameLibraryStatsApp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        classes = GameLibraryStatsApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class AuthKarateRunnerTest {

    @LocalServerPort
    private int port;

    @BeforeAll
    void beforeAll() {
        System.setProperty("baseUrl", "http://localhost:" + port);
    }

    @Karate.Test
    Karate auth() {
        return Karate.run("classpath:karate/auth/auth.feature");
    }
}