package com.gamelibrary.Tests.controller;

import com.gamelibrary.stats.controller.LauncherController;
import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.repository.LauncherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class LauncherControllerTest {

    private LauncherRepository launcherRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        launcherRepository = mock(LauncherRepository.class);
        LauncherController controller = new LauncherController(launcherRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/launchers returns DTO list")
    void getAll_returns_list() throws Exception {
        Launcher a = new Launcher(); a.setId(1L); a.setName("Steam");
        Launcher b = new Launcher(); b.setId(2L); b.setName("EA");
        when(launcherRepository.findAll()).thenReturn(List.of(a,b));

        mockMvc.perform(get("/api/launchers"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Steam")))
                .andExpect(content().string(containsString("EA")));
    }

    @Test
    @DisplayName("POST /api/launchers 400 when name missing")
    void create_bad_request_missing_name() throws Exception {
        mockMvc.perform(post("/api/launchers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Launcher name is required")));
        verify(launcherRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/launchers 400 when duplicate name")
    void create_duplicate_name() throws Exception {
        when(launcherRepository.findByName("Steam")).thenReturn(Optional.of(new Launcher()));
        mockMvc.perform(post("/api/launchers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" Steam \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Launcher already exists")));
        verify(launcherRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/launchers ok when valid")
    void create_success() throws Exception {
        when(launcherRepository.findByName("Steam")).thenReturn(Optional.empty());
        when(launcherRepository.save(any(Launcher.class))).thenAnswer(inv -> {
            Launcher l = inv.getArgument(0);
            l.setId(10L);
            return l;
        });
        mockMvc.perform(post("/api/launchers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Steam\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Steam")));
    }

    @Test
    @DisplayName("PUT /api/launchers/{id} 404 when not found")
    void update_not_found() throws Exception {
        when(launcherRepository.findById(5L)).thenReturn(Optional.empty());
        mockMvc.perform(put("/api/launchers/5").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/launchers/{id} 400 when name missing")
    void update_bad_request_missing_name() throws Exception {
        mockMvc.perform(put("/api/launchers/5").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Launcher name is required")));
    }

    @Test
    @DisplayName("PUT /api/launchers/{id} 400 when name conflicts with another")
    void update_conflict() throws Exception {
        Launcher existing = new Launcher(); existing.setId(5L); existing.setName("Old");
        when(launcherRepository.findById(5L)).thenReturn(Optional.of(existing));
        Launcher other = new Launcher(); other.setId(99L); other.setName("New");
        when(launcherRepository.findByName("New")).thenReturn(Optional.of(other));

        mockMvc.perform(put("/api/launchers/5").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Launcher name already exists")));
        verify(launcherRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/launchers/{id} ok when valid")
    void update_success() throws Exception {
        Launcher existing = new Launcher(); existing.setId(5L); existing.setName("Old");
        when(launcherRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(launcherRepository.findByName("New")).thenReturn(Optional.empty());
        when(launcherRepository.save(any(Launcher.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/launchers/5").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New")));
    }

    @Test
    @DisplayName("DELETE /api/launchers/{id} 404 when not found")
    void delete_not_found() throws Exception {
        when(launcherRepository.existsById(7L)).thenReturn(false);
        mockMvc.perform(delete("/api/launchers/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/launchers/{id} ok when exists")
    void delete_success() throws Exception {
        when(launcherRepository.existsById(7L)).thenReturn(true);
        mockMvc.perform(delete("/api/launchers/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Deleted")));
        verify(launcherRepository).deleteById(7L);
    }
}
