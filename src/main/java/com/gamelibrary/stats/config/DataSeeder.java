package com.gamelibrary.stats.config;

import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.gamelibrary.stats.repository.UserRepository;
import com.gamelibrary.stats.service.GameService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final LauncherRepository launcherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GameService gameService;

    public DataSeeder(
            LauncherRepository launcherRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GameService gameService
    ) {
        this.launcherRepository = launcherRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.gameService = gameService;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1) Seed launchers
        if (launcherRepository.count() == 0) {
            Arrays.asList("Steam", "Epic", "Xbox", "GOG", "Ubisoft").forEach(name -> {
                Launcher l = new Launcher();
                l.setName(name);
                launcherRepository.save(l);
            });
            System.out.println("Database seeded with default PC Launchers.");
        }

        // 2) Seed a demo admin user (only if missing)
        String demoUsername = "admin";
        User demo = userRepository.findByUsername(demoUsername).orElseGet(() -> {
            User u = new User();
            u.setUsername(demoUsername);
            u.setPassword(passwordEncoder.encode("admin"));
            u.setRole("ROLE_ADMIN");
            return userRepository.save(u);
        });

        // 3) Seed sample games ONLY if demo user has none
        if (gameService.countGamesForUser(demo.getUsername()) == 0) {
            String result = gameService.importGamesFromClasspathForUser("GameData2.csv", demo.getUsername());
            System.out.println(result);
        } else {
            System.out.println("Demo user already has games. Skipping CSV seed.");
        }
    }
}