package com.gamelibrary.gamelibrarystats.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Launcher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., "Steam", "Epic Games"

    @OneToMany(mappedBy = "launcher", cascade = CascadeType.ALL)
    private List<Game> games;
}