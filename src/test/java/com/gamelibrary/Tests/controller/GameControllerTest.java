package com.gamelibrary.Tests.controller;

import com.gamelibrary.stats.dto.GameDTO;
import com.gamelibrary.stats.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.gamelibrary.stats.GameLibraryStatsApp;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;


import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(classes = GameLibraryStatsApp.class)
@AutoConfigureMockMvc
@WithMockUser(username = "alice")
class GameControllerTest {

    @MockBean
    private GameService gameService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/games returns list for authenticated user")
    void getMyGames_ok() throws Exception {
        when(gameService.getMyGames("alice")).thenReturn(List.of(
                new GameDTO(1L, "Halo", "Steam", 10.0, "PLAYED", 19.99, "Action"),
                new GameDTO(2L, "Portal", "Steam", 5.5, "COMPLETED", 9.99, "Puzzle")
        ));

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Halo")))
                .andExpect(content().string(containsString("Portal")));

        verify(gameService).getMyGames("alice");
    }

    @Test
    @DisplayName("GET /api/games/{id}/cover returns existing URL")
    void getCover_found() throws Exception {
        when(gameService.getOrFetchCoverForUser(5L, "alice")).thenReturn(java.util.Optional.of("http://img/c.jpg"));

        mockMvc.perform(get("/api/games/5/cover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverUrl").value("http://img/c.jpg"));
    }

    @Test
    @DisplayName("GET /api/games/{id}/cover returns empty when not found")
    void getCover_empty() throws Exception {
        when(gameService.getOrFetchCoverForUser(6L, "alice")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/games/6/cover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverUrl").value(""));
    }

    @Test
    @DisplayName("POST /api/games/upload returns result from service")
    void upload_ok() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "games.csv", "text/csv", "Title,Launcher\nG1,Steam".getBytes());
        when(gameService.importGames(any(org.springframework.web.multipart.MultipartFile.class), eq("alice"), eq(true))).thenReturn("Imported");

        mockMvc.perform(multipart("/api/games/upload").file(file)
                        .param("excludeNonFull", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("Imported"));

        verify(gameService).importGames(any(org.springframework.web.multipart.MultipartFile.class), eq("alice"), eq(true));
    }

    @Test
    @DisplayName("POST /api/games/upload returns 500 on exception")
    void upload_error() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "games.csv", "text/csv", "Title,Launcher\nG1,Steam".getBytes());
        when(gameService.importGames(any(org.springframework.web.multipart.MultipartFile.class), eq("alice"), eq(false))).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(multipart("/api/games/upload").file(file)
                        .param("excludeNonFull", "false"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error processing file")));
    }
}
