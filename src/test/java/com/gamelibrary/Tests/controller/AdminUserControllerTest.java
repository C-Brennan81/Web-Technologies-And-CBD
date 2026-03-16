package com.gamelibrary.Tests.controller;

import com.gamelibrary.stats.controller.AdminUserController;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.UserRepository;
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

class AdminUserControllerTest {

    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        AdminUserController controller = new AdminUserController(userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/admin/users returns sanitized list")
    void list_users() throws Exception {
        User u1 = new User(); u1.setId(1L); u1.setUsername("a"); u1.setRole("ROLE_USER"); u1.setEnabled(true); u1.setPassword("HASH");
        User u2 = new User(); u2.setId(2L); u2.setUsername("b"); u2.setRole("ROLE_ADMIN"); u2.setEnabled(false);
        when(userRepository.findAll()).thenReturn(List.of(u1,u2));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"username\":\"a\"")))
                .andExpect(content().string(containsString("\"role\":\"ROLE_ADMIN\"")))
                .andExpect(content().string(containsString("\"enabled\":false")))
                .andExpect(content().string(containsString("\"id\":2")));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role 400 when role missing or blank")
    void update_role_missing() throws Exception {
        mockMvc.perform(patch("/api/admin/users/5/role").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Role is required")));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role 400 when invalid role")
    void update_role_invalid() throws Exception {
        mockMvc.perform(patch("/api/admin/users/5/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"guest\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Role must be ROLE_USER or ROLE_ADMIN")));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role 404 when user not found")
    void update_role_not_found() throws Exception {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/api/admin/users/5/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role ok when valid")
    void update_role_success() throws Exception {
        User u = new User(); u.setId(5L); u.setUsername("a"); u.setRole("ROLE_USER");
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/admin/users/5/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Role updated")));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/enabled 404 when user not found")
    void update_enabled_not_found() throws Exception {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/api/admin/users/9/enabled").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/enabled toggles and persists")
    void update_enabled_success() throws Exception {
        User u = new User(); u.setId(9L); u.setEnabled(false);
        when(userRepository.findById(9L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/admin/users/9/enabled").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User enabled")));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} 404 when not found")
    void delete_not_found() throws Exception {
        when(userRepository.existsById(11L)).thenReturn(false);
        mockMvc.perform(delete("/api/admin/users/11"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} ok when exists")
    void delete_success() throws Exception {
        when(userRepository.existsById(11L)).thenReturn(true);
        mockMvc.perform(delete("/api/admin/users/11"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User and their data deleted")));
        verify(userRepository).deleteById(11L);
    }
}
