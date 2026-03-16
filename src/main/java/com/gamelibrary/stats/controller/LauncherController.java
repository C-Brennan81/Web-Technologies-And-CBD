package com.gamelibrary.stats.controller;

import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.repository.LauncherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/launchers")
public class LauncherController {

    private final LauncherRepository launcherRepository;

    public LauncherController(LauncherRepository launcherRepository) {
        this.launcherRepository = launcherRepository;
    }

    private record LauncherDTO(Long id, String name) {}

    private LauncherDTO toDto(Launcher l) {
        return new LauncherDTO(l.getId(), l.getName());
    }

    @GetMapping
    public List<LauncherDTO> getAll() {
        return launcherRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Launcher req) {
        if (req == null || req.getName() == null || req.getName().trim().isBlank()) {
            return ResponseEntity.badRequest().body("Launcher name is required");
        }

        String name = req.getName().trim();
        if (launcherRepository.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body("Launcher already exists");
        }

        Launcher l = new Launcher();
        l.setName(name);
        var saved = launcherRepository.save(l);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Long id, @RequestBody Launcher req) {
        if (req == null || req.getName() == null || req.getName().trim().isBlank()) {
            return ResponseEntity.badRequest().body("Launcher name is required");
        }

        var existingOpt = launcherRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        String newName = req.getName().trim();

        // prevent renaming into an existing name
        var byName = launcherRepository.findByName(newName);
        if (byName.isPresent() && !byName.get().getId().equals(id)) {
            return ResponseEntity.badRequest().body("Launcher name already exists");
        }

        Launcher existing = existingOpt.get();
        existing.setName(newName);
        var saved = launcherRepository.save(existing);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        if (!launcherRepository.existsById(id)) return ResponseEntity.notFound().build();
        launcherRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
}