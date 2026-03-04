package com.gamelibrary.stats.repository;

import com.gamelibrary.stats.model.Launcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LauncherRepository extends JpaRepository<Launcher, Long> {
    // Custom method to find a launcher by name during CSV import
    Optional<Launcher> findByName(String name);
    Optional<Launcher> findByNameIgnoreCase(String name);
}