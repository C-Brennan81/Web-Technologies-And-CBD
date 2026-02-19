package com.gamelibrary.stats.model;

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
    private String name; // e.g., Steam, Epic, Xbox [cite: 10]

    // One launcher can have many games
    @OneToMany(mappedBy = "launcher", cascade = CascadeType.ALL)
    private List<Game> games;
}