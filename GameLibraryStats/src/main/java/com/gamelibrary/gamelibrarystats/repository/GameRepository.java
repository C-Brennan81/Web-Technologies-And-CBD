package com.gamelibrary.gamelibrarystats.repository;

import com.gamelibrary.gamelibrarystats.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByUserId(Long userId);
}