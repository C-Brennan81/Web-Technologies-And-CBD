package com.gamelibrary.gamelibrarystats.service;

import com.gamelibrary.gamelibrarystats.dto.GameDTO;
import com.gamelibrary.gamelibrarystats.model.Game;
import com.gamelibrary.gamelibrarystats.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    // Converts Database Entities to DTOs for the API
    public List<GameDTO> getAllGamesAsDTO() {
        return gameRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private GameDTO convertToDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setTitle(game.getTitle());
        dto.setGenre(game.getGenre());
        dto.setStatus(game.getStatus());
        // Handling the relationship safely
        if (game.getLauncher() != null) {
            dto.setLauncherName(game.getLauncher().getName());
        }
        if (game.getUser() != null) {
            dto.setUsername(game.getUser().getUsername());
        }
        return dto;
    }

    public String importGames(List<GameDTO> gameList) {
        int successCount = 0;
        int failCount = 0;

        for (GameDTO dto : gameList) {
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                failCount++;
                continue; // Skip any bad rows
            }

            // Save valid games logic goes here
            successCount++;
        }

        return "Import Complete: " + successCount + " success, " + failCount + " failed.";
    }
}