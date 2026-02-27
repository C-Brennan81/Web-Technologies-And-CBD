package com.gamelibrary.stats.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SteamGridDbClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${steamgriddb.apiKey}")
    private String apiKey;

    /**
     * Returns a single best cover URL (grid) for a game name, or null if not found.
     */
    public String findCoverUrlByName(String gameName) {
        Integer gameId = searchGameId(gameName);
        if (gameId == null) return null;

        return fetchBestGrid(gameId);
    }

    private Integer searchGameId(String gameName) {
        String encoded = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        String url = "https://www.steamgriddb.com/api/v2/search/autocomplete/" + encoded;

        Map<?, ?> json = getJson(url);
        if (json == null) return null;

        Object dataObj = json.get("data");
        if (!(dataObj instanceof List<?> data) || data.isEmpty()) return null;

        Object first = data.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return null;

        Object idObj = firstMap.get("id");
        if (idObj instanceof Number n) return n.intValue();

        return null;
    }

    private String fetchBestGrid(int gameId) {
        // You can try other grid sizes/styles; this is a good default.
        String url = "https://www.steamgriddb.com/api/v2/grids/game/" + gameId + "?dimensions=600x900";

        Map<?, ?> json = getJson(url);
        if (json == null) return null;

        Object dataObj = json.get("data");
        if (!(dataObj instanceof List<?> data) || data.isEmpty()) return null;

        // pick the first image returned
        Object first = data.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return null;

        Object urlObj = firstMap.get("url");
        return (urlObj instanceof String s) ? s : null;
    }

    private Map<?, ?> getJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (!resp.getStatusCode().is2xxSuccessful()) return null;
        return resp.getBody();
    }
}
