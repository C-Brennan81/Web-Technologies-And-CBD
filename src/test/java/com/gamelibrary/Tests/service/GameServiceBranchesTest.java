package com.gamelibrary.Tests.service;

import com.gamelibrary.stats.model.Game;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameServiceBranchesTest {

    @Mock private GameRepository gameRepository;
    @Mock private LauncherRepository launcherRepository;
    @Mock private SteamGridDbClient steamGridDbClient;
    @Mock private UserRepository userRepository;

    @InjectMocks private GameService gameService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
    }

    @Test
    @DisplayName("importGames: skips row with missing title and missing source")
    void import_skips_missing_title_and_source() throws Exception {
        String csv = String.join("\n",
                "Name,Source,Genres,Status,Time Played",
                ",,Action,Completed,100",
                "Some Title,,Action,Completed,100"
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        String result = gameService.importGames(is, "alice", true);
        assertTrue(result.contains("Successfully saved 0 games"), result);
        assertTrue(result.contains("Skipped 2 rows"), result);
        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("importGames: duplicate rows are skipped")
    void import_skips_duplicates() throws Exception {
        String csv = String.join("\n",
                "Name,Source,Genres,Status,Time Played",
                "Halo,Steam,Action,Completed,3600"
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        when(gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(eq("alice"), anyString(), nullable(String.class)))
                .thenReturn(true); // pretend already exists
        // need a launcher created when code tries to resolve
        when(launcherRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(new Launcher()));

        String result = gameService.importGames(is, "alice", true);
        assertTrue(result.contains("Successfully saved 0 games"), result);
        assertTrue(result.contains("Skipped 1 rows"), result);
        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("importGames: include non-full releases when excludeNonFull=false")
    void import_includes_non_full_when_flag_false() throws Exception {
        String csv = String.join("\n",
                "Name,Source,Genres,Status,Time Played",
                "Cool Game Demo,EA App,Action,Playing,100"
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(eq("alice"), anyString(), anyString()))
                .thenReturn(false);
        Launcher ea = new Launcher();
        ea.setId(2L);
        ea.setName("EA");
        when(launcherRepository.findByNameIgnoreCase("EA")).thenReturn(Optional.of(ea));

        String result = gameService.importGames(is, "alice", false);
        assertTrue(result.contains("Successfully saved 1 games"), result);
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    @Test
    @DisplayName("importGames: launcher alias normalization maps to expected names")
    void import_launcher_aliases() throws Exception {
        String csv = String.join("\n",
                "Title,Launcher,Genres,Status,Playtime",
                "Game1,Origin,Action,Completed,10",
                "Game2,Microsoft Store,Action,Completed,10",
                "Game3,Ubisoft Connect,Action,Completed,10"
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(eq("alice"), anyString(), anyString()))
                .thenReturn(false);

        // Return empty so service creates/saves new launchers with normalized names
        when(launcherRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(launcherRepository.save(any(Launcher.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = gameService.importGames(is, "alice", true);
        assertTrue(result.contains("Successfully saved 3 games"), result);
        verify(launcherRepository, atLeast(1)).save(argThat(l ->
                l.getName().equals("EA") || l.getName().equals("Xbox") || l.getName().equals("Ubisoft")
        ));
    }

    @Test
    @DisplayName("getMyGames: converts seconds to hours and maps platform")
    void get_my_games_converts_hours() {
        // Prepare a Game entity with 7200 seconds and launcher name
        Game g = new Game();
        g.setTitle("T1");
        g.setTimePlayed(7200);
        Launcher l = new Launcher();
        l.setName("Steam");
        g.setLauncher(l);
        when(gameRepository.findByUserUsername("alice")).thenReturn(java.util.List.of(g));

        var list = gameService.getMyGames("alice");
        assertEquals(1, list.size());
        assertEquals(2.0, list.get(0).getPlayTimeHours());
        assertEquals("Steam", list.get(0).getPlatform());
    }
}
