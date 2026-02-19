package com.gamelibrary.stats.config;

import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.gamelibrary.stats.service.GameService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final LauncherRepository launcherRepository;
    private final GameService gameService;

    public DataSeeder(LauncherRepository launcherRepository, GameService gameService) {
        this.launcherRepository = launcherRepository;
        this.gameService = gameService;
    }

    @Override
    public void run(String... args) throws Exception {

        // Seed launchers first (so platform mapping works)
        if (launcherRepository.count() == 0) {
            Arrays.asList("Steam", "Epic", "Xbox", "GOG", "Ubisoft").forEach(name -> {
                Launcher l = new Launcher();
                l.setName(name);
                launcherRepository.save(l);
            });
            System.out.println("Database seeded with default PC Launchers.");
        }

        // Wipe + import CSV every startup
        String result = gameService.replaceAllGamesFromClasspath("GameData2.csv");
        System.out.println(result);
    }
}
