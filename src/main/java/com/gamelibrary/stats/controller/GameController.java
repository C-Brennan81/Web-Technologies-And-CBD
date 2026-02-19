package com.gamelibrary.stats.controller;

import com.gamelibrary.stats.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gamelibrary.stats.dto.GameDTO;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    // Upload CSV
    @PostMapping("/upload")
    public ResponseEntity<String> uploadGames(@RequestParam("file") MultipartFile file) {
        try {
            String result = gameService.importGames(file);
            return ResponseEntity.ok(result); // Should return a success notif
        } catch (Exception e) {
            // Error handling if the import doesn't work
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }

    @GetMapping
    public List<GameDTO> getGames() {
        return gameService.getAllGames();
    }
}