package com.gamelibrary.stats.controller;

import com.gamelibrary.stats.dto.GameDTO;
import com.gamelibrary.stats.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<GameDTO> getMyGames(Authentication auth) {
        return gameService.getMyGames(auth.getName());
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Map<String, String>> getCover(@PathVariable Long id, Authentication auth) {
        return gameService.getOrFetchCoverForUser(id, auth.getName())
                .map(url -> ResponseEntity.ok(Map.of("coverUrl", url)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("coverUrl", "")));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadGames(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "excludeNonFull", defaultValue = "true") boolean excludeNonFull,
            Authentication auth
    ) {
        try {
            String result = gameService.importGames(file, auth.getName(), excludeNonFull);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }
}