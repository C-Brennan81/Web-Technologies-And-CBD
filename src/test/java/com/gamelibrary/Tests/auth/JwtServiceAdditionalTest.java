package com.gamelibrary.Tests.auth;

import com.gamelibrary.stats.auth.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceAdditionalTest {

    @Test
    @DisplayName("isValid -> false when token is expired")
    void expired_token_invalid() throws InterruptedException {
        String secret = "0123456789abcdef0123456789abcdef";
        // 0 minutes means immediate expiration; to be safe, use 0 and expect invalid
        JwtService jwt = new JwtService(secret, 0);
        String token = jwt.generateToken("carol", "ROLE_USER");
        // Immediately invalid due to zero expiration
        assertFalse(jwt.isValid(token));
    }

    @Test
    @DisplayName("isValid -> false on null/empty token")
    void empty_token_invalid() {
        String secret = "0123456789abcdef0123456789abcdef";
        JwtService jwt = new JwtService(secret, 5);
        assertFalse(jwt.isValid(null));
        assertFalse(jwt.isValid(""));
        assertFalse(jwt.isValid("   "));
    }
}
