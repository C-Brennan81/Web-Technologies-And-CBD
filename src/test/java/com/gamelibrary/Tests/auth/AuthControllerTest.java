package com.gamelibrary.Tests.auth;

import com.gamelibrary.stats.auth.AuthController;
import com.gamelibrary.stats.auth.jwt.JwtService;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        AuthController controller = new AuthController(userRepository, passwordEncoder, authenticationManager, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("register: 400 when username/password missing")
    void register_missing_fields_bad_request() throws Exception {
        String body = "{\n  \"username\": \"\", \n  \"password\": null\n}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username and password are required")));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: 400 when username already exists")
    void register_duplicate_username() throws Exception {
        when(userRepository.existsByUsername("craig")).thenReturn(true);
        String body = "{\n  \"username\": \"craig\", \n  \"password\": \"pw\"\n}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username already taken")));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: 200 and user persisted when ok")
    void register_success() throws Exception {
        when(userRepository.existsByUsername("craig")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        String body = "{\n  \"username\": \" craig \", \n  \"password\": \"pw\"\n}";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registered")));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("craig", saved.getUsername());
        assertEquals("ENC", saved.getPassword());
        assertEquals("ROLE_USER", saved.getRole());
    }

    @Test
    @DisplayName("login: 401 on bad credentials")
    void login_bad_credentials() throws Exception {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        String body = "{\n  \"username\": \"u\", \n  \"password\": \"p\"\n}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid credentials")));
    }

    @Test
    @DisplayName("login: 200 with token on success")
    void login_success() throws Exception {
        // authManager.authenticate does nothing (success)
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(new User() {{ setUsername("u"); setRole("ROLE_USER"); }}));
        when(jwtService.generateToken("u", "ROLE_USER")).thenReturn("TKN");

        String body = "{\n  \"username\": \"u\", \n  \"password\": \"p\"\n}";
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TKN")));
    }
}
