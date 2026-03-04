package com.gamelibrary.stats.service;

import com.gamelibrary.stats.dto.GameDTO;
import com.gamelibrary.stats.model.Game;
import com.gamelibrary.stats.model.Launcher;
import com.gamelibrary.stats.repository.GameRepository;
import com.gamelibrary.stats.repository.LauncherRepository;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gamelibrary.stats.model.User;
import com.gamelibrary.stats.repository.UserRepository;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired private GameRepository gameRepository;
    @Autowired private LauncherRepository launcherRepository;
    @Autowired private SteamGridDbClient steamGridDbClient;
    @Autowired private UserRepository userRepository;

    private final Random random = new Random();

    // Upload endpoint uses this
    // Upload endpoint uses this
    public String importGames(MultipartFile file, String username, boolean excludeNonFull) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return importGames(is, username, excludeNonFull);
        }
    }

    // Backward compatible default
    public String importGames(MultipartFile file, String username) throws Exception {
        return importGames(file, username, true);
    }

    public String importGames(InputStream inputStream, String username, boolean excludeNonFull) throws Exception {
        int savedCount = 0;
        int skippedCount = 0;

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] header = reader.readNext(); // header
            if (header == null) return "Empty CSV";

            // Build a normalized header name -> index map (tolerant to variants)
            java.util.Map<String,Integer> idx = new java.util.HashMap<>();
            for (int i=0; i<header.length; i++) {
                String key = normalizeHeader(header[i]);
                if (!key.isBlank()) idx.put(key, i);
            }

            // Row loop
            String[] currentLine;
            while ((currentLine = reader.readNext()) != null) {
                String title = firstNonNull(
                        safe(currentLine, idx.getOrDefault("name", -1)),
                        safe(currentLine, idx.getOrDefault("title", -1))
                );
                if (title == null || title.isBlank()) {
                    skippedCount++;
                    continue;
                }

                if (excludeNonFull && isNonFullRelease(title)) {
                    skippedCount++;
                    continue;
                }

                Game game = new Game();
                game.setTitle(title.trim());
                game.setUser(user);

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

                // Time Played: assume seconds if large; attempt parse as integer
                Integer timeVal = parseIntSafe(firstNonNull(
                        safe(currentLine, idx.getOrDefault("timeplayed", -1)),
                        safe(currentLine, idx.getOrDefault("playtime", -1))
                ));
                game.setTimePlayed(timeVal);

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

                // Resolve or create launcher (case-insensitive)
                Optional<Launcher> launcherOpt = launcherRepository.findByNameIgnoreCase(normalizedLauncherName);
                Launcher launcher = launcherOpt.orElseGet(() -> {
                    Launcher l = new Launcher();
                    l.setName(normalizedLauncherName);
                    return launcherRepository.save(l);
                });
                game.setLauncher(launcher);

                double price = 5.0 + (55.0 * random.nextDouble());
                game.setPurchasePrice(Math.round(price * 100.0) / 100.0);

                game.setAgeRating(firstNonNull(
                        safe(currentLine, idx.getOrDefault("agerating", -1)),
                        safe(currentLine, idx.getOrDefault("age", -1))
                ));
                game.setDevelopers(safe(currentLine, idx.getOrDefault("developers", -1)));
                game.setPublishers(safe(currentLine, idx.getOrDefault("publishers", -1)));
                game.setCommunityScore(parseDoubleSafe(safe(currentLine, idx.getOrDefault("communityscore", -1))));

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
                .map(g -> {
                    String platform = (g.getLauncher() != null) ? g.getLauncher().getName() : null;
                    Double hours = (g.getTimePlayed() == null) ? null : (g.getTimePlayed() / 3600.0);

                    GameDTO dto = new GameDTO(
                            g.getId(),
                            g.getTitle(),
                            platform,
                            hours,
                            g.getCompletionStatus(),
                            g.getPurchasePrice(),
                            g.getGenre()
                    );

                    dto.setCoverUrl(g.getCoverUrl());
                    return dto;
                })
                .collect(Collectors.toList());
    }




    private static String safe(String[] line, int index) {
        if (line == null || index < 0 || index >= line.length) return null;
        String v = line[index];
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String firstGenre(String genres) {
        if (genres == null || genres.isBlank()) return null;
        String[] parts = genres.split(",");
        return parts.length > 0 ? parts[0].trim() : genres.trim();
    }

    private static Integer parseIntSafe(String v) {
        try {
            if (v == null || v.isBlank()) return null;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDoubleSafe(String v) {
        try {
            if (v == null || v.isBlank()) return null;
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    //Helpers
    public long countGamesForUser(String username) {
        return gameRepository.findByUserUsername(username).size();
    }

    private static boolean isNonFullRelease(String title) {
        if (title == null) return false;
        String t = normalizeTitle(title);

        // keep these simple and obvious for marking
        return t.contains(" demo")
                || t.contains(" beta")
                || t.contains(" playtest")
                || t.contains(" alpha")
                || t.contains(" trial");
    }

    private static String normalizeTitle(String title) {
        String t = title.toLowerCase();

        // replace non-alphanumeric with spaces
        t = t.replaceAll("[^a-z0-9]+", " ");

        // collapse multiple spaces
        t = t.replaceAll("\\s+", " ").trim();

        // pad with spaces so "demo" matches cleanly with contains(" demo")
        return " " + t + " ";
    }

    public Optional<String> getOrFetchCoverForUser(Long id, String username) {
        return gameRepository.findByIdAndUserUsername(id, username).map(g -> {
            String coverUrl = g.getCoverUrl();
            if (coverUrl == null || coverUrl.isBlank()) {
                String found = steamGridDbClient.findCoverUrlByName(g.getTitle());
                if (found != null && !found.isBlank()) {
                    g.setCoverUrl(found);
                    gameRepository.save(g);
                    coverUrl = found;
                }
            }
            return coverUrl;
        });
    }

    // ---- Helpers for tolerant CSV parsing ----
    private static String normalizeHeader(String h) {
        if (h == null) return "";
        String k = h.toLowerCase();
        // remove non-alphanumeric characters
        k = k.replaceAll("[^a-z0-9]+", "");
        return k.trim();
    }

    private static String firstNonNull(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String normalizeCompletionStatus(String status) {
        if (status == null || status.isBlank()) return "NOT PLAYED";
        String s = status.trim().toUpperCase();
        if (s.contains("NOT") && s.contains("PLAYED")) return "NOT PLAYED";
        if (s.contains("PLAYED") && !s.contains("NOT")) return "PLAYED";
        if (s.contains("COMPLETE")) return "COMPLETED";
        return s;
    }

    private static String normalizeLauncherName(String name) {
        if (name == null) return null;
        String n = name.trim();
        // Common aliases mapping
        if (n.equalsIgnoreCase("Ubisoft Connect")) return "Ubisoft";
        if (n.equalsIgnoreCase("EA App") || n.equalsIgnoreCase("Origin")) return "EA";
        if (n.equalsIgnoreCase("Microsoft Store") || n.equalsIgnoreCase("Xbox")) return "Xbox";
        return n;
    }
}
