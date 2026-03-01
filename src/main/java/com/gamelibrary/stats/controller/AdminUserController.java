package com.gamelibrary.stats.controller;

import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepo;

    public AdminUserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // DTO so we don’t leak password hash
    public record AdminUserRow(Long id, String username, String role, boolean enabled) {}

    public record UpdateRoleRequest(String role) {}
    public record UpdateEnabledRequest(boolean enabled) {}

    @GetMapping
    public List<AdminUserRow> listUsers() {
        return userRepo.findAll().stream()
                .map(u -> new AdminUserRow(u.getId(), u.getUsername(), u.getRole(), u.isEnabled()))
                .toList();
    }

    @PatchMapping("/{id}/role")
    @Transactional
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest req) {
        if (req == null || req.role() == null || req.role().isBlank()) {
            return ResponseEntity.badRequest().body("Role is required");
        }

        String role = req.role().trim().toUpperCase();
        if (!role.equals("ROLE_USER") && !role.equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest().body("Role must be ROLE_USER or ROLE_ADMIN");
        }

        var userOpt = userRepo.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        user.setRole(role);
        userRepo.save(user);

        return ResponseEntity.ok("Role updated");
    }

    @PatchMapping("/{id}/enabled")
    @Transactional
    public ResponseEntity<?> updateEnabled(@PathVariable Long id, @RequestBody UpdateEnabledRequest req) {
        var userOpt = userRepo.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        user.setEnabled(req.enabled());
        userRepo.save(user);

        return ResponseEntity.ok(req.enabled() ? "User enabled" : "User disabled");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();

        // If your User has cascade+orphanRemoval on games, this deletes their games too
        userRepo.deleteById(id);

        return ResponseEntity.ok("User and their data deleted");
    }
}