package com.gamelibrary.stats.repository;

import com.gamelibrary.stats.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // Basic CRUD methods are inherited automatically
}