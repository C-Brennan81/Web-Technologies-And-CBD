package com.gamelibrary.gamelibrarystats.controller;

import com.gamelibrary.gamelibrarystats.dto.GameDTO;
import com.gamelibrary.gamelibrarystats.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping
    public List<GameDTO> getAllGames() {
        // Now returns the DTOs, satisfying requirement
        return gameService.getAllGamesAsDTO();
    }
}