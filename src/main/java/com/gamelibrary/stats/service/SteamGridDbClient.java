package com.gamelibrary.stats.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class SteamGridDbClient {

    private static final Logger log = LoggerFactory.getLogger(SteamGridDbClient.class);

    private final RestTemplate restTemplate;

    @Value("${steamgriddb.apiKey}")
    private String apiKey;

    public SteamGridDbClient() {
        this(new RestTemplate());
    }

    public SteamGridDbClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Returns a single best cover URL (grid) for a game name, or null if not found.
     */
    public String findCoverUrlByName(String gameName) {
        try {
            Integer gameId = searchGameId(gameName);
            if (gameId == null) {
                log.debug("[SteamGridDB] No gameId found for '{}'", gameName);
                return null;
            }

            String url = fetchBestGrid(gameId);
            if (url == null) {
                url = fetchBestGrid(gameId, false);
            }
            if (url == null) {
                log.debug("[SteamGridDB] No grid found for '{}' (id={})", gameName, gameId);
            }
            return url;
        } catch (Exception e) {
            log.warn("[SteamGridDB] Error while fetching cover for '{}': {}", gameName, e.toString());
            return null;
        }
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
        return fetchBestGrid(gameId, true);
    }

    private String fetchBestGrid(int gameId, boolean withDimensions) {
        String url = "https://www.steamgriddb.com/api/v2/grids/game/" + gameId
                + (withDimensions ? "?dimensions=600x900" : "");

        Map<?, ?> json = getJson(url);
        if (json == null) return null;

        Object dataObj = json.get("data");
        if (!(dataObj instanceof List<?> data) || data.isEmpty()) return null;

        Object first = data.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return null;

        Object urlObj = firstMap.get("url");
        if (urlObj instanceof String s && !s.isBlank()) return s;

        Object thumbObj = firstMap.get("thumb");
        return (thumbObj instanceof String ts && !ts.isBlank()) ? ts : null;
    }

    private Map<?, ?> getJson(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("[SteamGridDB] Non-2xx status {} for URL {}", resp.getStatusCode(), url);
                return null;
            }
            return resp.getBody();
        } catch (RestClientException ex) {
            log.warn("[SteamGridDB] HTTP error for URL {}: {}", url, ex.toString());
            return null;
        } catch (Exception ex) {
            log.warn("[SteamGridDB] Unexpected error for URL {}: {}", url, ex.toString());
            return null;
        }
    }
}