package com.gamelibrary.Tests.config;

import com.gamelibrary.stats.config.DataSeeder;
import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.gamelibrary.stats.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DataSeederTest {

    private LauncherRepository launcherRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        launcherRepository = mock(LauncherRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        dataSeeder = new DataSeeder(
                launcherRepository,
                userRepository,
                passwordEncoder
        );

        ReflectionTestUtils.setField(dataSeeder, "seedAdminUsername", "admin");
        ReflectionTestUtils.setField(dataSeeder, "seedAdminPassword", "admin");
    }

    @Test
    @DisplayName("run seeds default launchers when repository is empty")
    void run_seeds_default_launchers_when_empty() throws Exception {
        when(launcherRepository.count()).thenReturn(0L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

        dataSeeder.run();

        ArgumentCaptor<Launcher> captor = ArgumentCaptor.forClass(Launcher.class);
        verify(launcherRepository, times(5)).save(captor.capture());

        List<String> savedNames = captor.getAllValues().stream()
                .map(Launcher::getName)
                .toList();

        assertEquals(List.of("Steam", "Epic", "Xbox", "GOG", "Ubisoft"), savedNames);
    }

    @Test
    @DisplayName("run seeds admin when missing")
    void run_seeds_admin_when_missing() throws Exception {
        when(launcherRepository.count()).thenReturn(1L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("ENC_ADMIN");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("ENC_ADMIN", saved.getPassword());
        assertEquals("ROLE_ADMIN", saved.getRole());
    }

    @Test
    @DisplayName("run updates existing admin when present")
    void run_updates_existing_admin_when_present() throws Exception {
        when(launcherRepository.count()).thenReturn(1L);

        User existing = new User();
        existing.setUsername("admin");
        existing.setPassword("OLD_PASSWORD");
        existing.setRole("ROLE_USER");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("admin")).thenReturn("ENC_ADMIN");

        dataSeeder.run();

        verify(passwordEncoder).encode("admin");
        verify(userRepository).save(existing);

        assertEquals("admin", existing.getUsername());
        assertEquals("ENC_ADMIN", existing.getPassword());
        assertEquals("ROLE_ADMIN", existing.getRole());
    }

    @Test
    @DisplayName("run does not seed launchers when repository already has entries")
    void run_does_not_seed_launchers_when_repository_not_empty() throws Exception {
        when(launcherRepository.count()).thenReturn(2L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(passwordEncoder.encode("admin")).thenReturn("ENC_ADMIN");

        dataSeeder.run();

        verify(launcherRepository, never()).save(any(Launcher.class));
    }
}