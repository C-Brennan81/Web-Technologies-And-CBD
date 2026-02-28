package com.gamelibrary.stats.service;

import com.gamelibrary.stats.dto.GameDTO;
import com.gamelibrary.stats.model.Game;
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

    public String importGames(InputStream inputStream, String username, boolean excludeNonFull) throws Exception {        int savedCount = 0;
        int skippedCount = 0;

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // header

            while ((line = reader.readNext()) != null) {

                String title = safe(line, 0);
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

                String genres = safe(line, 4);
                game.setGenres(genres);
                game.setGenre(firstGenre(genres));

                String completion = safe(line, 6);
                game.setCompletionStatus(completion);
                game.setStatus(completion);

                Integer seconds = parseIntSafe(safe(line, 7));
                game.setTimePlayed(seconds);


                //Parsing/ Skip Lines
                String source = safe(line, 8);
                if (source == null || source.isBlank()) {
                    skippedCount++;
                    continue;
                }

                var launcherOpt = launcherRepository.findByName(source.trim());
                if (launcherOpt.isEmpty()) {
                    skippedCount++;
                    continue;
                }
                game.setLauncher(launcherOpt.get());

                double price = 5.0 + (55.0 * random.nextDouble());
                game.setPurchasePrice(Math.round(price * 100.0) / 100.0);

                game.setAgeRating(safe(line, 1));
                game.setDevelopers(safe(line, 2));
                game.setPublishers(safe(line, 3));
                game.setCommunityScore(parseDoubleSafe(safe(line, 9)));


                if (gameRepository.gameAlreadyExists(
                        username, game.getTitle(), game.getLauncher().getName()
                )) {
                    skippedCount++;
                    continue;
                }

                gameRepository.save(game);
                savedCount++;
            }
        }

        return "Import complete! Successfully saved " + savedCount + " games. Skipped " + skippedCount + " bad rows.";
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

}
