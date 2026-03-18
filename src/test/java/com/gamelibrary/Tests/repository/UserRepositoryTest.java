package com.gamelibrary.Tests.repository;

import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.gamelibrary.stats.GameLibraryStatsApp.class)
@ActiveProfiles("test")
 class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("existsByUsername returns true when user present, false otherwise")
    void exists_by_username() {
        assertFalse(userRepository.existsByUsername("alice"));
        User u = new User();
        u.setUsername("alice");
        u.setPassword("x");
        u.setRole("ROLE_USER");
        userRepository.save(u);
        assertTrue(userRepository.existsByUsername("alice"));
        assertFalse(userRepository.existsByUsername("bob"));
    }
}
