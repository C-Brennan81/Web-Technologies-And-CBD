package com.gamelibrary.Tests.service;

import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.GameRepository;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.gamelibrary.stats.repository.UserRepository;
import com.gamelibrary.stats.service.GameService;
import com.gamelibrary.stats.service.SteamGridDbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GameServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private LauncherRepository launcherRepository;
    @Mock private SteamGridDbClient steamGridDbClient;
    @Mock private UserRepository userRepository;

    @InjectMocks private GameService gameService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("importGames(InputStream): saves 1 game and skips incomplete/demo")
    void import_games_basic_flow() throws Exception {
        // Given CSV with header and two rows: one valid full release, one demo
        String csv = String.join("\n",
                "Name,Genres,Source,Time Played,Status",
                "Halo,Action,Steam,3600,Completed",
                "Cool Game Demo,Action,GOG,120,Playing"
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        // Mocks
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        when(gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(eq("alice"), anyString(), anyString()))
                .thenReturn(false);

        Launcher steam = new Launcher();
        steam.setId(10L);
        steam.setName("Steam");
        when(launcherRepository.findByNameIgnoreCase("Steam")).thenReturn(Optional.of(steam));

        // When
        String result = gameService.importGames(is, "alice", true);

        // Then
        assertTrue(result.contains("Successfully saved 1 games"), result);
        assertTrue(result.contains("Skipped 1 rows"), result);
        verify(gameRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("getMyGames: maps repository results to DTOs without NPE")
    void get_my_games_maps_dto() {
        // minimal: repository returns empty list
        when(gameRepository.findByUserUsername("bob")).thenReturn(List.of());
        assertNotNull(gameService.getMyGames("bob"));
        assertEquals(0, gameService.getMyGames("bob").size());
    }
}
