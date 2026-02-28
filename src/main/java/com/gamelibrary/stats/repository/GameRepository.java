package com.gamelibrary.stats.repository;

import com.gamelibrary.stats.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByUserUsername(String username);

    Optional<Game> findByIdAndUserUsername(Long id, String username);

    boolean gameAlreadyExists(String username, String title, String launcherName);
}

