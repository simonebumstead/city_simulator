package it.citylife.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gestisce I/O JSON dei salvataggi: scrive nella cartella {@code saves/} e
 * delega a {@link SaveDataMapper} l'estrazione dello stato e a
 * {@link SaveDataApplier} la ricostruzione al caricamento.
 *
 * @see SaveDataMapper
 * @see SaveDataApplier
 */
public class SaveLoadManager {

    static final Path SAVES_DIR = Path.of(System.getProperty("user.dir"), "saves");

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ObjectMapper mapper = new ObjectMapper();

    public Path saveAuto(City city, int tick) throws IOException {
        Files.createDirectories(SAVES_DIR);
        String filename = "save_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".json";
        Path file = SAVES_DIR.resolve(filename);
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(file.toFile(), SaveDataMapper.toSaveData(city, tick));
        return file;
    }

    public List<Path> listSaves() throws IOException {
        if (!Files.exists(SAVES_DIR)) return List.of();
        try (Stream<Path> stream = Files.list(SAVES_DIR)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    public int load(City city, Path path) throws IOException {
        SaveData data = mapper.readValue(path.toFile(), SaveData.class);
        SaveDataApplier.apply(city, data);
        return data.tick;
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public static class SaveData {
        public int    tick;
        public String activePolicy;
        public double budget;
        public int    population;
        public double pollution;
        public double happiness;
        public double health;
        public int    wasteLevel;
        public double jobSatisfaction = 50.0;
        public double healthSatisfaction = 50.0;
        public double safetySatisfaction = 50.0;
        public int    totalProduction;
        public int    totalConsumption;
        public List<BuildingEntry> buildings;
    }

    public static class BuildingEntry {
        public String       type;
        public int          x;
        public int          y;
        public int          hp = -1;
        public List<String> upgrades = new ArrayList<>();
    }
}
