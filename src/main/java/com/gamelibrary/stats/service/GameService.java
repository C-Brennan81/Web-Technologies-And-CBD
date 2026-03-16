package com.gamelibrary.stats.service;

import com.gamelibrary.stats.dto.GameDTO;
import com.gamelibrary.stats.model.Game;
import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.GameRepository;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.gamelibrary.stats.repository.UserRepository;
import com.opencsv.CSVReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class GameService {

    private static final String PLAYED = "PLAYED";
    private static final String NOT_PLAYED = "NOT PLAYED";
    private static final String COMPLETED = "COMPLETED";

    private final GameRepository gameRepository;
    private final LauncherRepository launcherRepository;
    private final SteamGridDbClient steamGridDbClient;
    private final UserRepository userRepository;
    private final Random random = new Random();

    public GameService(
            GameRepository gameRepository,
            LauncherRepository launcherRepository,
            SteamGridDbClient steamGridDbClient,
            UserRepository userRepository
    ) {
        this.gameRepository = gameRepository;
        this.launcherRepository = launcherRepository;
        this.steamGridDbClient = steamGridDbClient;
        this.userRepository = userRepository;
    }

    public String importGames(MultipartFile file, String username, boolean excludeNonFull) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return importGames(is, username, excludeNonFull);
        }
    }

    public String importGames(MultipartFile file, String username) throws Exception {
        return importGames(file, username, true);
    }

    public String importGames(InputStream inputStream, String username, boolean excludeNonFull) throws Exception {
        int savedCount = 0;
        int skippedCount = 0;

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null) {
                return "Empty CSV";
            }

            Map<String, Integer> idx = buildHeaderIndex(header);

            String[] currentLine;
            while ((currentLine = reader.readNext()) != null) {
                String title = firstNonNull(
                        safe(currentLine, idx.getOrDefault("name", -1)),
                        safe(currentLine, idx.getOrDefault("title", -1))
                );

                if (shouldSkipRow(title, excludeNonFull)) {
                    skippedCount++;
                    continue;
                }

                String sourceRaw = firstNonNull(
                        safe(currentLine, idx.getOrDefault("sources", -1)),
                        safe(currentLine, idx.getOrDefault("source", -1)),
                        safe(currentLine, idx.getOrDefault("launcher", -1)),
                        safe(currentLine, idx.getOrDefault("platform", -1))
                );

                if (sourceRaw == null || sourceRaw.isBlank()) {
                    skippedCount++;
                    continue;
                }

                String normalizedLauncherName = normalizeLauncherName(sourceRaw);
                Launcher launcher = resolveLauncher(normalizedLauncherName);

                Game game = buildGame(currentLine, idx, user, title, launcher);

                if (gameRepository.existsByUserUsernameAndTitleAndLauncher_Name(
                        username, game.getTitle(), game.getLauncher().getName()
                )) {
                    skippedCount++;
                    continue;
                }

                gameRepository.save(game);
                savedCount++;
            }
        }

        return "Import complete! Successfully saved " + savedCount + " games. Skipped " + skippedCount + " rows.";
    }

    public String importGames(InputStream inputStream, String username) throws Exception {
        return importGames(inputStream, username, true);
    }

    @Transactional
    public String importGamesFromClasspathForUser(String classpathCsvName, String username) throws Exception {
        ClassPathResource csv = new ClassPathResource(classpathCsvName);
        try (InputStream is = csv.getInputStream()) {
            return importGames(is, username);
        }
    }

    public List<GameDTO> getMyGames(String username) {
        return gameRepository.findByUserUsername(username)
                .stream()
                .map(game -> {
                    String platform = game.getLauncher() != null ? game.getLauncher().getName() : null;
                    Double hours = game.getTimePlayed() == null ? null : game.getTimePlayed() / 3600.0;

                    GameDTO dto = new GameDTO(
                            game.getId(),
                            game.getTitle(),
                            platform,
                            hours,
                            game.getCompletionStatus(),
                            game.getPurchasePrice(),
                            game.getGenre()
                    );

                    dto.setCoverUrl(game.getCoverUrl());
                    return dto;
                })
                .toList();
    }

    public long countGamesForUser(String username) {
        return gameRepository.findByUserUsername(username).size();
    }

    public Optional<String> getOrFetchCoverForUser(Long id, String username) {
        return gameRepository.findByIdAndUserUsername(id, username).map(game -> {
            String coverUrl = game.getCoverUrl();

            if (coverUrl == null || coverUrl.isBlank()) {
                String found = steamGridDbClient.findCoverUrlByName(game.getTitle());
                if (found != null && !found.isBlank()) {
                    game.setCoverUrl(found);
                    gameRepository.save(game);
                    coverUrl = found;
                }
            }

            return coverUrl;
        });
    }

    private static Map<String, Integer> buildHeaderIndex(String[] header) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String key = normalizeHeader(header[i]);
            if (!key.isBlank()) {
                idx.put(key, i);
            }
        }
        return idx;
    }

    private static boolean shouldSkipRow(String title, boolean excludeNonFull) {
        if (title == null || title.isBlank()) {
            return true;
        }
        return excludeNonFull && isNonFullRelease(title);
    }

    private Game buildGame(
            String[] currentLine,
            Map<String, Integer> idx,
            User user,
            String title,
            Launcher launcher
    ) {
        Game game = new Game();
        game.setTitle(title.trim());
        game.setUser(user);
        game.setLauncher(launcher);

        String genres = firstNonNull(
                safe(currentLine, idx.getOrDefault("genres", -1)),
                safe(currentLine, idx.getOrDefault("genre", -1))
        );
        game.setGenres(genres);
        game.setGenre(firstGenre(genres));

        String completionRaw = firstNonNull(
                safe(currentLine, idx.getOrDefault("completionstatus", -1)),
                safe(currentLine, idx.getOrDefault("status", -1))
        );
        String completion = normalizeCompletionStatus(completionRaw);
        game.setCompletionStatus(completion);
        game.setStatus(completion);

        Integer timeVal = parseIntSafe(firstNonNull(
                safe(currentLine, idx.getOrDefault("timeplayed", -1)),
                safe(currentLine, idx.getOrDefault("playtime", -1))
        ));
        game.setTimePlayed(timeVal);

        double price = 5.0 + (55.0 * random.nextDouble());
        game.setPurchasePrice(Math.round(price * 100.0) / 100.0);

        game.setAgeRating(firstNonNull(
                safe(currentLine, idx.getOrDefault("agerating", -1)),
                safe(currentLine, idx.getOrDefault("age", -1))
        ));
        game.setDevelopers(safe(currentLine, idx.getOrDefault("developers", -1)));
        game.setPublishers(safe(currentLine, idx.getOrDefault("publishers", -1)));
        game.setCommunityScore(parseDoubleSafe(safe(currentLine, idx.getOrDefault("communityscore", -1))));

        return game;
    }

    private Launcher resolveLauncher(String normalizedLauncherName) {
        return launcherRepository.findByNameIgnoreCase(normalizedLauncherName)
                .orElseGet(() -> {
                    Launcher launcher = new Launcher();
                    launcher.setName(normalizedLauncherName);
                    return launcherRepository.save(launcher);
                });
    }

    private static String safe(String[] line, int index) {
        if (line == null || index < 0 || index >= line.length) {
            return null;
        }
        String value = line[index];
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String firstGenre(String genres) {
        if (genres == null || genres.isBlank()) {
            return null;
        }
        String[] parts = genres.split(",");
        return parts.length > 0 ? parts[0].trim() : genres.trim();
    }

    private static Integer parseIntSafe(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDoubleSafe(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isNonFullRelease(String title) {
        if (title == null) {
            return false;
        }
        String normalized = normalizeTitle(title);
        return normalized.contains(" demo")
                || normalized.contains(" beta")
                || normalized.contains(" playtest")
                || normalized.contains(" alpha")
                || normalized.contains(" trial");
    }

    private static String normalizeTitle(String title) {
        String normalized = title.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9]+", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return " " + normalized + " ";
    }

    private static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String normalized = header.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9]+", "");
        return normalized.trim();
    }

    private static String firstNonNull(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String normalizeCompletionStatus(String status) {
        if (status == null || status.isBlank()) {
            return NOT_PLAYED;
        }

        String normalized = status.trim().toUpperCase();

        if (normalized.contains("NOT") && normalized.contains(PLAYED)) {
            return NOT_PLAYED;
        }
        if (normalized.contains(PLAYED) && !normalized.contains("NOT")) {
            return PLAYED;
        }
        if (normalized.contains("COMPLETE")) {
            return COMPLETED;
        }

        return normalized;
    }

    private static String normalizeLauncherName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();

        if (normalized.equalsIgnoreCase("Ubisoft Connect")) {
            return "Ubisoft";
        }
        if (normalized.equalsIgnoreCase("EA App") || normalized.equalsIgnoreCase("Origin")) {
            return "EA";
        }
        if (normalized.equalsIgnoreCase("Microsoft Store") || normalized.equalsIgnoreCase("Xbox")) {
            return "Xbox";
        }

        return normalized;
    }
}