package com.gamelibrary.stats.config;

import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.gamelibrary.stats.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final LauncherRepository launcherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:admin}")
    private String seedAdminUsername;

    @Value("${app.seed.admin.password:ChangeMe123!}")
    private String seedAdminPassword;

    public DataSeeder(
            LauncherRepository launcherRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.launcherRepository = launcherRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (launcherRepository.count() == 0) {
            Arrays.asList("Steam", "Epic", "Xbox", "GOG", "Ubisoft").forEach(name -> {
                Launcher launcher = new Launcher();
                launcher.setName(name);
                launcherRepository.save(launcher);
            });
            log.info("Database seeded with default PC launchers.");
        }

        userRepository.findByUsername(seedAdminUsername).orElseGet(() -> {
            User user = new User();
            user.setUsername(seedAdminUsername);
            user.setPassword(passwordEncoder.encode(seedAdminPassword));
            user.setRole("ROLE_ADMIN");
            return userRepository.save(user);
        });
    }
}