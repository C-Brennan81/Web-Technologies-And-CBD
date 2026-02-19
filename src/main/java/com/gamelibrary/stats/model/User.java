package com.gamelibrary.stats.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role; // e.g., "ROLE_USER" or "ROLE_ADMIN"

    // One user can have many games (Inverse side of the relationship)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Game> games;
}