package it.citylife.model.save;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.citylife.model.core.City;

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

    private Path saveToFile(City city, int tick, String filename) throws IOException {
        Files.createDirectories(SAVES_DIR);
        Path file = SAVES_DIR.resolve(filename);
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(file.toFile(), SaveDataMapper.toSaveData(city, tick));
        return file;
    }

    /**
     * Salva manualmente la partita con timestamp nel nome del file.
     * @param city la città da serializzare
     * @param tick il tick corrente da includere nel salvataggio
     * @return il percorso del file JSON creato
     * @throws IOException se la scrittura su disco fallisce
     */
    public Path saveManual(City city, int tick) throws IOException {
        String filename = "save_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".json";
        return saveToFile(city, tick, filename);
    }

    /**
     * Esegue un salvataggio automatico (autosave) identificato dalla sessionId.
     * @param city      la città da serializzare
     * @param tick      il tick corrente
     * @param sessionId identificatore univoco della sessione di gioco
     * @return il percorso del file JSON creato/sovrascritto
     * @throws IOException se la scrittura su disco fallisce
     */
    public Path saveAuto(City city, int tick, String sessionId) throws IOException {
        String filename = "autosave_" + sessionId + ".json";
        return saveToFile(city, tick, filename);
    }

    /**
     * Restituisce la lista dei file di salvataggio presenti nella cartella {@code saves/}.
     * @return lista di percorsi ordinati per nome; lista vuota se la cartella non esiste
     * @throws IOException se la lettura della directory fallisce
     */
    public List<Path> listSaves() throws IOException {
        if (!Files.exists(SAVES_DIR)) return List.of();
        try (Stream<Path> stream = Files.list(SAVES_DIR)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    /**
     * Carica una partita da file JSON e applica lo stato alla città fornita.
     * @param city la città su cui ripristinare lo stato
     * @param path il percorso del file di salvataggio
     * @return il numero di tick al momento del salvataggio
     * @throws IOException se il file non esiste, non è leggibile o contiene JSON malformato
     */
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
