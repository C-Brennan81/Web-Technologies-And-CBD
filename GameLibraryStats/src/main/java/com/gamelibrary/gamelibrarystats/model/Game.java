package com.gamelibrary.gamelibrarystats.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String genre;
    private String status;

    @ManyToOne
    @JoinColumn(name = "launcher_id")
    private Launcher launcher;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}