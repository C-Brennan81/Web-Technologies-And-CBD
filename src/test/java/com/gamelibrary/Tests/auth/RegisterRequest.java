package com.gamelibrary.Tests.auth;

import com.gamelibrary.stats.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void gettersAndSettersWork() {
        RegisterRequest request = new RegisterRequest();

        request.setUsername("craig");
        request.setPassword("secret");

        assertEquals("craig", request.getUsername());
        assertEquals("secret", request.getPassword());
    }
}