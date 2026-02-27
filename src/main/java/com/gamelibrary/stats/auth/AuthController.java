package com.gamelibrary.stats.auth;

import com.gamelibrary.stats.auth.dto.*;
import com.gamelibrary.stats.auth.jwt.JwtService;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepo,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtService jwtService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
        if (userRepo.existsByUsername(req.username.trim())) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User u = new User();
        u.setUsername(req.username.trim());
        u.setPassword(encoder.encode(req.password));
        u.setRole("ROLE_USER");

        userRepo.save(u);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username, req.password)
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        var user = userRepo.findByUsername(req.username).orElseThrow();
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}