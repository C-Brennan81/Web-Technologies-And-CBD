package com.gamelibrary.stats.service;

import com.gamelibrary.stats.model.*;
import com.gamelibrary.stats.repository.*;
import com.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import com.gamelibrary.stats.dto.GameDTO;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private LauncherRepository launcherRepository;
    private final Random random = new Random();

    public String importGames(MultipartFile file) throws Exception {
        int savedCount = 0;
        int skippedCount = 0;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            reader.readNext(); // header

            while ((line = reader.readNext()) != null) {

                // Name
                if (line[0] == null || line[0].isBlank()) {
                    skippedCount++;
                    continue;
                }

                Game game = new Game();
                game.setTitle(line[0]);

                // Full fields from CSV
                game.setAgeRating(emptyToNull(line[1]));
                game.setDevelopers(emptyToNull(line[2]));
                game.setPublishers(emptyToNull(line[3]));
                game.setGenres(emptyToNull(line[4]));
                game.setCompletionStatus(emptyToNull(line[6]));

                // Last Played (optional)
                if (line[5] != null && !line[5].isBlank()) {
                    game.setLastPlayed(LocalDateTime.parse(line[5], dtf));
                }

                // Time Played (seconds)
                if (line[7] != null && !line[7].isBlank()) {
                    game.setTimePlayed(Integer.parseInt(line[7]));
                }

                // Community Score
                if (line[9] != null && !line[9].isBlank()) {
                    game.setCommunityScore(Double.parseDouble(line[9]));
                }

                // Launcher comes from Sources column (index 8)
                String source = emptyToNull(line[8]);
                if (source != null) {
                    launcherRepository.findByName(source).ifPresent(game::setLauncher);
                }

                // Keep your enrichment price if you want
                double price = 5.0 + (55.0 * random.nextDouble());
                game.setPurchasePrice(Math.round(price * 100.0) / 100.0);

                gameRepository.save(game);
                savedCount++;
            }
        }

        return "Import complete! Successfully saved " + savedCount + " games. Skipped " + skippedCount + " bad rows.";
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public List<GameDTO> getAllGames() {
        return gameRepository.findAll()
                .stream()
                .map(g -> new GameDTO(
                        g.getId(),
                        g.getTitle(),
                        g.getGenre(),
                        g.getStatus(),
                        g.getLauncher() != null ? g.getLauncher().getName() : null,
                        g.getPurchasePrice()
                ))
                .collect(Collectors.toList());
    }


}
