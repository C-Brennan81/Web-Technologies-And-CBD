package com.gamelibrary.Tests.service;

import com.gamelibrary.stats.service.SteamGridDbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

 class SteamGridDbClientTest {

    private RestTemplate restTemplate;
    private SteamGridDbClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new SteamGridDbClient(restTemplate);
        ReflectionTestUtils.setField(client, "apiKey", "fake-api-key");
    }

    @Test
    @DisplayName("findCoverUrlByName returns primary url when search and grid succeed")
    void findCoverUrl_returns_primary_url() {
        Map<String, Object> searchBody = Map.of(
                "data", List.of(Map.of("id", 123))
        );
        Map<String, Object> gridBody = Map.of(
                "data", List.of(Map.of("url", "http://img/halo.jpg", "thumb", "http://img/thumb.jpg"))
        );

        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(searchBody, HttpStatus.OK));

        when(restTemplate.exchange(contains("/grids/game/123?dimensions=600x900"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(gridBody, HttpStatus.OK));

        String result = client.findCoverUrlByName("Halo");

        assertEquals("http://img/halo.jpg", result);
    }

    @Test
    @DisplayName("findCoverUrlByName falls back to thumb when url is blank")
    void findCoverUrl_falls_back_to_thumb() {
        Map<String, Object> searchBody = Map.of(
                "data", List.of(Map.of("id", 321))
        );
        Map<String, Object> gridBody = Map.of(
                "data", List.of(Map.of("url", "   ", "thumb", "http://img/thumb-only.jpg"))
        );

        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(searchBody, HttpStatus.OK));

        when(restTemplate.exchange(contains("/grids/game/321?dimensions=600x900"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(gridBody, HttpStatus.OK));

        String result = client.findCoverUrlByName("Portal");

        assertEquals("http://img/thumb-only.jpg", result);
    }

    @Test
    @DisplayName("findCoverUrlByName retries without dimensions when first grid lookup returns empty")
    void findCoverUrl_retries_without_dimensions() {
        Map<String, Object> searchBody = Map.of(
                "data", List.of(Map.of("id", 456))
        );
        Map<String, Object> emptyGridBody = Map.of("data", List.of());
        Map<String, Object> fallbackGridBody = Map.of(
                "data", List.of(Map.of("url", "http://img/fallback.jpg"))
        );

        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(searchBody, HttpStatus.OK));

        when(restTemplate.exchange(contains("/grids/game/456?dimensions=600x900"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(emptyGridBody, HttpStatus.OK));

        when(restTemplate.exchange(eq("https://www.steamgriddb.com/api/v2/grids/game/456"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(fallbackGridBody, HttpStatus.OK));

        String result = client.findCoverUrlByName("Mass Effect");

        assertEquals("http://img/fallback.jpg", result);
    }

    @Test
    @DisplayName("findCoverUrlByName returns null when search finds no game id")
    void findCoverUrl_returns_null_when_search_empty() {
        Map<String, Object> searchBody = Map.of("data", List.of());

        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(searchBody, HttpStatus.OK));

        String result = client.findCoverUrlByName("Unknown Game");

        assertNull(result);
        verify(restTemplate, times(1))
                .exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("findCoverUrlByName returns null when both grid lookups fail")
    void findCoverUrl_returns_null_when_both_grid_calls_fail() {
        Map<String, Object> searchBody = Map.of(
                "data", List.of(Map.of("id", 777))
        );
        Map<String, Object> emptyGridBody = Map.of("data", List.of());

        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(searchBody, HttpStatus.OK));

        when(restTemplate.exchange(contains("/grids/game/777"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(emptyGridBody, HttpStatus.OK));

        String result = client.findCoverUrlByName("No Cover Game");

        assertNull(result);
    }

    @Test
    @DisplayName("findCoverUrlByName returns null when HTTP call throws RestClientException")
    void findCoverUrl_returns_null_on_rest_client_exception() {
        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("boom"));

        String result = client.findCoverUrlByName("Halo");

        assertNull(result);
    }

    @Test
    @DisplayName("client sends bearer token header")
    void getJson_sends_bearer_auth_header() {
        Map<String, Object> searchBody = Map.of("data", List.of());
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(contains("/search/autocomplete/"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(searchBody, HttpStatus.OK));

        client.findCoverUrlByName("Halo");

        verify(restTemplate).exchange(
                contains("/search/autocomplete/"),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(Map.class)
        );

        HttpEntity captured = entityCaptor.getValue();
        HttpHeaders headers = captured.getHeaders();

        assertEquals(List.of(MediaType.APPLICATION_JSON), headers.getAccept());
        assertEquals("Bearer fake-api-key", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }
}