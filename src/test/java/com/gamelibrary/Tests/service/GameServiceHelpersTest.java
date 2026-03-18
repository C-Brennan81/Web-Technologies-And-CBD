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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameServiceHelpersTest {

    @Mock private GameRepository gameRepository;
    @Mock private LauncherRepository launcherRepository;
    @Mock private SteamGridDbClient steamGridDbClient;
    @Mock private UserRepository userRepository;

    @InjectMocks private GameService gameService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
    }

    @Test
    @DisplayName("importGames maps completion status variants to canonical values")
    void import_maps_completion_status() throws Exception {
        String csv = String.join("\n",
                "Title,Launcher,Status",
                "G1,Steam,not played",
                "G2,Steam,Played",
                "G3,Steam,complete"
        );
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        Launcher steam = new Launcher();
        steam.setId(10L);
        steam.setName("Steam");
        when(launcherRepository.findByNameIgnoreCase("Steam")).thenReturn(Optional.of(steam));
        when(gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(eq("alice"), anyString(), anyString()))
                .thenReturn(false);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        String res = gameService.importGames(is, "alice", true);
        assertTrue(res.contains("Successfully saved 3 games"), res);

        verify(gameRepository, times(3)).save(captor.capture());
        List<Game> saved = captor.getAllValues();
        assertEquals("NOT PLAYED", saved.get(0).getCompletionStatus());
        assertEquals("NOT PLAYED", saved.get(0).getStatus());
        assertEquals("PLAYED", saved.get(1).getCompletionStatus());
        assertEquals("COMPLETED", saved.get(2).getCompletionStatus());
    }

    @Test
    @DisplayName("importGames parses numbers and tolerant headers")
    void import_parses_numbers_and_headers() throws Exception {
        String csv = String.join("\n",
                "Name,Platform,Completion Status,PlayTime,Community Score,Age-Rating,Developers,Publishers",
                "GameX,Steam,Played, 7200 , 8.5 , PEGI 16 , DevA , PubA "
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        Launcher steam = new Launcher();
        steam.setId(10L);
        steam.setName("Steam");
        when(launcherRepository.findByNameIgnoreCase("Steam")).thenReturn(Optional.of(steam));
        when(gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(eq("alice"), anyString(), anyString()))
                .thenReturn(false);
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = gameService.importGames(is, "alice", true);
        assertTrue(result.contains("Successfully saved 1 games"), result);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        Game g = captor.getValue();
        assertEquals(7200, g.getTimePlayed());
        assertEquals(8.5, g.getCommunityScore());
        assertEquals("PEGI 16", g.getAgeRating());
        assertEquals("DevA", g.getDevelopers());
        assertEquals("PubA", g.getPublishers());
    }

    @Test
    @DisplayName("importGames skips non-full releases for multiple keywords when excludeNonFull=true")
    void import_skips_various_non_full() throws Exception {
        String csv = String.join("\n",
                "Title,Launcher,Status",
                "Great Game Beta,Steam,Played",
                "Great Game Playtest,Steam,Played",
                "Great Game Alpha,Steam,Played",
                "Great Game Trial,Steam,Played"
        );
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        String result = gameService.importGames(is, "alice", true);
        assertTrue(result.contains("Successfully saved 0 games"), result);
        assertTrue(result.contains("Skipped 4 rows"), result);
        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrFetchCoverForUser fetches and persists when missing")
    void getOrFetch_fetches_and_persists() {
        Game g = new Game();
        g.setId(5L);
        g.setTitle("Halo");
        g.setCoverUrl(null);

        when(gameRepository.findByIdAndUserUsername(5L, "alice")).thenReturn(Optional.of(g));
        when(steamGridDbClient.findCoverUrlByName("Halo")).thenReturn("http://img/halo.jpg");

        Optional<String> url = gameService.getOrFetchCoverForUser(5L, "alice");
        assertTrue(url.isPresent());
        assertEquals("http://img/halo.jpg", url.get());
        verify(gameRepository, times(1)).save(g);
    }

    @Test
    @DisplayName("getOrFetchCoverForUser returns existing without external call")
    void getOrFetch_returns_existing() {
        Game g = new Game();
        g.setId(7L);
        g.setTitle("Portal");
        g.setCoverUrl("http://img/portal.jpg");

        when(gameRepository.findByIdAndUserUsername(7L, "alice")).thenReturn(Optional.of(g));

        Optional<String> url = gameService.getOrFetchCoverForUser(7L, "alice");
        assertTrue(url.isPresent());
        assertEquals("http://img/portal.jpg", url.get());
        verifyNoInteractions(steamGridDbClient);
        verify(gameRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrFetchCoverForUser returns empty when game not found")
    void getOrFetch_not_found() {
        when(gameRepository.findByIdAndUserUsername(99L, "alice")).thenReturn(Optional.empty());
        Optional<String> url = gameService.getOrFetchCoverForUser(99L, "alice");
        assertTrue(url.isEmpty());
    }

    @Test
    @DisplayName("countGamesForUser delegates to repository")
    void count_games_for_user() {
        when(gameRepository.findByUserUsername("alice")).thenReturn(java.util.List.of(new Game(), new Game()));
        assertEquals(2, gameService.countGamesForUser("alice"));
    }
}
