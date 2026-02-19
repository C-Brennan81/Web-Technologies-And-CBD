package com.gamelibrary.stats.controller;

import com.gamelibrary.gamelibrarystats.dto.GameDTO;
import com.gamelibrary.gamelibrarystats.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/games")
@CrossOrigin // Allows your frontend to talk to your backend
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping // Richardson Level 2: GET for retrieval
    public List<GameDTO> getGames() {
        return gameService.getAllGames();
    }
}