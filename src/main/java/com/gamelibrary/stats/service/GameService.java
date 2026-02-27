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

    private final Random random = new Random();

    // Upload endpoint uses this
    public String importGames(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return importGames(is);
        }
    }

    // Startup seeding uses this (and MultipartFile delegates to it)
    public String importGames(InputStream inputStream) throws Exception {
        int savedCount = 0;
        int skippedCount = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] line;
            reader.readNext(); // header

            while ((line = reader.readNext()) != null) {

                String title = safe(line, 0);
                if (title == null || title.isBlank()) {
                    skippedCount++;
                    continue;
                }

                Game game = new Game();
                game.setTitle(title.trim());

                String genres = safe(line, 4);
                game.setGenres(genres);
                game.setGenre(firstGenre(genres));

                String completion = safe(line, 6);
                game.setCompletionStatus(completion);
                game.setStatus(completion); // keep compatibility

                Integer seconds = parseIntSafe(safe(line, 7));
                game.setTimePlayed(seconds);

                String source = safe(line, 8);
                if (source != null) {
                    launcherRepository.findByName(source.trim()).ifPresent(game::setLauncher);
                }

                // Price enrichment
                double price = 5.0 + (55.0 * random.nextDouble());
                game.setPurchasePrice(Math.round(price * 100.0) / 100.0);

                // Optional extra fields (you already have these on Game)
                game.setAgeRating(safe(line, 1));
                game.setDevelopers(safe(line, 2));
                game.setPublishers(safe(line, 3));
                game.setCommunityScore(parseDoubleSafe(safe(line, 9)));

                gameRepository.save(game);
                savedCount++;
            }
        }

        return "Import complete! Successfully saved " + savedCount + " games. Skipped " + skippedCount + " bad rows.";
    }

    @Transactional
    public String replaceAllGamesFromClasspath(String classpathCsvName) throws Exception {
        gameRepository.deleteAll();
        ClassPathResource csv = new ClassPathResource(classpathCsvName);
        try (InputStream is = csv.getInputStream()) {
            return importGames(is);
        }
    }

    public List<GameDTO> getAllGames() {
        return gameRepository.findAll()
                .stream()
                .map(g -> {
                    String platform = (g.getLauncher() != null) ? g.getLauncher().getName() : null;
                    Double hours = (g.getTimePlayed() == null) ? null : (g.getTimePlayed() / 3600.0);

                    return new GameDTO(
                            g.getId(),
                            g.getTitle(),
                            platform,
                            hours,
                            g.getCompletionStatus(),
                            g.getPurchasePrice(),
                            g.getGenre()
                    );
                })
                .collect(java.util.stream.Collectors.toList());
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

    public Optional<String> getOrFetchCover(Long id) {
        return gameRepository.findById(id).map(g -> {
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
