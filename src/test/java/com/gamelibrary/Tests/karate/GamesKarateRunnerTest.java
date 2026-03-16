package com.gamelibrary.Tests.karate;

import com.intuit.karate.junit5.Karate;
import com.gamelibrary.stats.GameLibraryStatsApp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = GameLibraryStatsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GamesKarateRunnerTest {

    @LocalServerPort int port;

    @BeforeAll
    void beforeAll() {
        System.setProperty("baseUrl", "http://localhost:" + port);
    }

    @Karate.Test
    Karate games() {
        return Karate.run("classpath:karate/games/games.feature");
    }
}