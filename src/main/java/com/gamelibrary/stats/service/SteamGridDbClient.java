package com.gamelibrary.stats.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

        Map<String, Object> json = getJson(url);
        if (json.isEmpty()) {
            return null;
        }

        Object dataObj = json.get("data");
        if (!(dataObj instanceof List<?> data) || data.isEmpty()) {
            return null;
        }

        Object first = data.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }

        Object idObj = firstMap.get("id");
        if (idObj instanceof Number number) {
            return number.intValue();
        }

        return null;
    }

    private String fetchBestGrid(int gameId) {
        return fetchBestGrid(gameId, true);
    }

    private String fetchBestGrid(int gameId, boolean withDimensions) {
        String url = "https://www.steamgriddb.com/api/v2/grids/game/" + gameId
                + (withDimensions ? "?dimensions=600x900" : "");

        Map<String, Object> json = getJson(url);
        if (json.isEmpty()) {
            return null;
        }

        Object dataObj = json.get("data");
        if (!(dataObj instanceof List<?> data) || data.isEmpty()) {
            return null;
        }

        Object first = data.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }

        Object urlObj = firstMap.get("url");
        if (urlObj instanceof String value && !value.isBlank()) {
            return value;
        }

        Object thumbObj = firstMap.get("thumb");
        if (thumbObj instanceof String value && !value.isBlank()) {
            return value;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJson(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[SteamGridDB] Non-2xx status {} for URL {}", response.getStatusCode(), url);
                return Collections.emptyMap();
            }

            return response.getBody() != null ? response.getBody() : Collections.emptyMap();
        } catch (RestClientException ex) {
            log.warn("[SteamGridDB] HTTP error for URL {}: {}", url, ex.toString());
            return Collections.emptyMap();
        } catch (Exception ex) {
            log.warn("[SteamGridDB] Unexpected error for URL {}: {}", url, ex.toString());
            return Collections.emptyMap();
        }
    }
}