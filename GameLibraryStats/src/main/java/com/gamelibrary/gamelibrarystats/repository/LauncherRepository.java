package com.gamelibrary.gamelibrarystats.repository;

import com.gamelibrary.gamelibrarystats.model.Launcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LauncherRepository extends JpaRepository<Launcher, Long> {
    Optional<Launcher> findByName(String name);
}