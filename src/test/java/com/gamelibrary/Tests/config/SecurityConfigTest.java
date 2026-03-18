package com.gamelibrary.Tests.config;

import com.gamelibrary.stats.GameLibraryStatsApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GameLibraryStatsApp.class)
@ActiveProfiles("test")
 class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("filterChain")
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("Security beans are created: PasswordEncoder and SecurityFilterChain")
    void security_beans_present() {
        assertNotNull(passwordEncoder);
        assertNotNull(securityFilterChain);
        // quick sanity: encoder encodes and matches
        String hash = passwordEncoder.encode("pw");
        assertTrue(passwordEncoder.matches("pw", hash));
    }
}
