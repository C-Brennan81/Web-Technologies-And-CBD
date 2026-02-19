package com.gamelibrary.stats.config;

import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.repository.LauncherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final LauncherRepository launcherRepository;

    public DataSeeder(LauncherRepository launcherRepository) {
        this.launcherRepository = launcherRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if the table is empty to avoid duplicates
        if (launcherRepository.count() == 0) {
            Arrays.asList("Steam", "Epic", "Xbox", "GOG", "Ubisoft").forEach(name -> {
                Launcher l = new Launcher();
                l.setName(name);
                launcherRepository.save(l);
            });
            System.out.println("Database seeded with default PC Launchers.");
        }
    }
}