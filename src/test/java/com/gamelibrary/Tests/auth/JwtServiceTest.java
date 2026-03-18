package com.gamelibrary.Tests.auth;

import com.gamelibrary.stats.auth.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

 class JwtServiceTest {

    @Test
    @DisplayName("generateToken -> valid & extractUsername works")
    void generate_and_validate_token() {
        String secret = "0123456789abcdef0123456789abcdef"; // 32+ chars
        JwtService jwt = new JwtService(secret, 5); // 5 minutes

        String token = jwt.generateToken("alice", "ROLE_USER");
        assertNotNull(token);
        assertTrue(jwt.isValid(token));
        assertEquals("alice", jwt.extractUsername(token));
    }

    @Test
    @DisplayName("isValid -> false on tampered token")
    void tampered_token_is_invalid() {
        String secret = "0123456789abcdef0123456789abcdef";
        JwtService jwt = new JwtService(secret, 1);
        String token = jwt.generateToken("bob", "ROLE_USER");
        // tamper by appending a char
        String bad = token + "x";
        assertFalse(jwt.isValid(bad));
    }
}
