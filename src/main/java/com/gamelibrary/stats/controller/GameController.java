package com.gamelibrary.stats.controller;

import com.gamelibrary.stats.dto.GameDTO;
import com.gamelibrary.stats.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping
    public List<GameDTO> getAllGames() {
        return gameService.getAllGames();
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Map<String, String>> getCover(@PathVariable Long id) {
        return gameService.getOrFetchCover(id)
                .map(url -> ResponseEntity.ok(Map.of("coverUrl", url)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("coverUrl", "")));
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadGames(@RequestParam("file") MultipartFile file) {
        try {
            String result = gameService.importGames(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }
}
