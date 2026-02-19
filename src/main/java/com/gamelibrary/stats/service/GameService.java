package com.gamelibrary.gamelibrarystats.service;

import com.gamelibrary.gamelibrarystats.dto.GameDTO;
import com.gamelibrary.gamelibrarystats.model.Game;
import com.gamelibrary.gamelibrarystats.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    private final Random random = new Random();

    public List<GameDTO> getAllGames() {
        return gameRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void saveGameWithEnrichment(Game game) {
        // US-04: Data Enrichment - Randomly generate price if missing [cite: 130, 132]
        if (game.getPurchasePrice() == null || game.getPurchasePrice() == 0) {
            double price = 5.0 + (55.0 * random.nextDouble());
            game.setPurchasePrice(Math.round(price * 100.0) / 100.0);
        }
        gameRepository.save(game);
    }

    private GameDTO convertToDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setTitle(game.getTitle());
        dto.setGenre(game.getGenre());
        dto.setStatus(game.getStatus());
        dto.setPurchasePrice(game.getPurchasePrice());
        // Handling the relationship for the frontend [cite: 22, 31]
        if (game.getLauncher() != null) {
            dto.setLauncherName(game.getLauncher().getName());
        }
        return dto;
    }
}