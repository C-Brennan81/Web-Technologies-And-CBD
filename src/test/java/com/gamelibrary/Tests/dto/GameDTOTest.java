package com.gamelibrary.Tests.dto;

import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.gamelibrary.stats.dto.GameDTO;

 class GameDTOTest {


        @Test
        void defaultConstructorAndSettersShouldWork() {
            GameDTO dto = new GameDTO();

            dto.setId(1L);
            dto.setTitle("Elden Ring");
            dto.setPlatform("Steam");
            dto.setPlayTimeHours(120.5);
            dto.setCompletionStatus("Completed");
            dto.setPurchasePrice(59.99);
            dto.setGenre("RPG");
            dto.setCoverUrl("cover.jpg");

            assertEquals(1L, dto.getId());
            assertEquals("Elden Ring", dto.getTitle());
            assertEquals("Steam", dto.getPlatform());
            assertEquals(120.5, dto.getPlayTimeHours());
            assertEquals("Completed", dto.getCompletionStatus());
            assertEquals(59.99, dto.getPurchasePrice());
            assertEquals("RPG", dto.getGenre());
            assertEquals("cover.jpg", dto.getCoverUrl());
        }

        @Test
        void allArgsConstructorShouldSetExpectedFields() {
            GameDTO dto = new GameDTO(
                    2L,
                    "Cyberpunk 2077",
                    "GOG",
                    87.0,
                    "In Progress",
                    39.99,
                    "Action RPG"
            );

            assertEquals(2L, dto.getId());
            assertEquals("Cyberpunk 2077", dto.getTitle());
            assertEquals("GOG", dto.getPlatform());
            assertEquals(87.0, dto.getPlayTimeHours());
            assertEquals("In Progress", dto.getCompletionStatus());
            assertEquals(39.99, dto.getPurchasePrice());
            assertEquals("Action RPG", dto.getGenre());
            assertNull(dto.getCoverUrl()); // not set by constructor
        }
    }
