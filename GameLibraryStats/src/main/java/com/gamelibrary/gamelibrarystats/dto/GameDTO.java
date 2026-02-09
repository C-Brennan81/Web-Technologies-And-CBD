package com.gamelibrary.gamelibrarystats.dto;

import lombok.Data;

@Data
public class GameDTO {
    private Long id;
    private String title;
    private String genre;
    private String status;
    private String launcherName;
    private String username;     // To show who owns the game
}