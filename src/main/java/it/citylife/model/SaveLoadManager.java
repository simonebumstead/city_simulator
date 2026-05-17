package it.citylife.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gestisce il salvataggio e il caricamento dello stato della città su file JSON.
 *
 * Ogni salvataggio produce un file con nome autogenerato basato sul timestamp
 * (es. {@code save_2026-05-13_14-30-00.json}) nella cartella {@code saves/}
 * relativa alla directory di lavoro corrente.
 *
 * Il formato di serializzazione usa due DTO interni:
 *   - {@link SaveData}: metriche globali, tick, politica attiva e lista degli edifici
 *   - {@link BuildingEntry}: tipo, coordinate e lista degli upgrade di ogni struttura
 *
 * Al caricamento, la griglia viene azzerata e ricostruita interamente:
 * ogni edificio viene ricreato tramite {@link BuildingFactory#createBuilding} e
 * gli upgrade riapplicati in ordine tramite {@link BuildingFactory#applyUpgrade},
 * ripristinando la catena di Decorator esattamente com'era al momento del salvataggio.
 *
 * @see BuildingFactory
 * @see GameController#saveGame(int)
 * @see GameController#loadGame(Path)
 */
public class SaveLoadManager {

    // Cartella di destinazione dei file di salvataggio
    static final Path SAVES_DIR = Path.of(System.getProperty("user.dir"), "saves");

    // Formato del timestamp usato nel nome del file di salvataggio
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // Serializzatore/deserializzatore JSON di Jackson
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Salva lo stato corrente della città su un nuovo file JSON con nome autogenerato.
     * La cartella saves/ viene creata automaticamente se non esiste.
     *
     * @param city la città da salvare
     * @param tick il numero di tick corrente da includere nel salvataggio
     * @return il Path del file JSON creato
     * @throws IOException se la scrittura su disco fallisce
     */
    public Path saveAuto(City city, int tick) throws IOException {
        Files.createDirectories(SAVES_DIR);
        String filename = "save_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".json";
        Path file = SAVES_DIR.resolve(filename);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), buildSaveData(city, tick));
        return file;
    }

    /**
     * Restituisce la lista dei file di salvataggio disponibili, ordinati per nome (cronologico).
     *
     * @return lista di Path dei file .json nella cartella saves/; lista vuota se la cartella non esiste
     * @throws IOException se la lettura della cartella fallisce
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
     * Carica un salvataggio dal file indicato, ripristinando griglia, metriche e politica.
     *
     * Sequenza di operazioni:
     *   1. Deserializza il file JSON in un SaveData
     *   2. Azzera l'intera griglia
     *   3. Ripristina le metriche di CityState tramite setter diretti
     *   4. Ricostruisce ogni edificio con BuildingFactory e riapplica gli upgrade in ordine
     *   5. Ripristina la politica attiva
     *
     * @param city la città su cui applicare il salvataggio
     * @param path il Path del file JSON da caricare
     * @return il numero di tick registrato nel salvataggio
     * @throws IOException se la lettura del file fallisce o il formato è invalido
     */
    public int load(City city, Path path) throws IOException {
        SaveData data = mapper.readValue(path.toFile(), SaveData.class);

        // Azzera completamente la griglia prima di ricostruirla
        Grid grid = city.getGrid();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                grid.removeStructure(x, y);
            }
        }

        // Ripristina le metriche tramite setter diretti (bypass del delta)
        CityState s = city.getState();
        s.setBudget(data.budget);
        s.setPopulation(data.population);
        s.setHappiness(data.happiness);
        s.setHealth(data.health);
        s.setPollution(data.pollution);
        s.setWasteLevel(data.wasteLevel);
        
        s.getPopulationGroup().setJobSatisfaction(data.jobSatisfaction);
        s.getPopulationGroup().setHealthSatisfaction(data.healthSatisfaction);
        s.getPopulationGroup().setSafetySatisfaction(data.safetySatisfaction);

        city.getPowerNet().reset();
        city.getPowerNet().addProduction(data.totalProduction);
        city.getPowerNet().addConsumption(data.totalConsumption);

        // Ricostruisce ogni edificio e riapplica gli upgrade in ordine (più interno → esterno)
        for (BuildingEntry entry : data.buildings) {
            Structure building = BuildingFactory.createBuilding(entry.type);
            
            // Ripristina gli HP se presenti nel salvataggio, altrimenti lascia full HP (retrocompatibilità vecchi save)
            if (entry.hp >= 0) {
                int damageToInflict = building.getMaxHp() - entry.hp;
                if (damageToInflict > 0) {
                    building.takeDamage(damageToInflict);
                }
            }

            if (entry.upgrades != null) {
                for (String upgrade : entry.upgrades) {
                    building = BuildingFactory.applyUpgrade(building, upgrade);
                }
            }
            grid.placeStructure(building, entry.x, entry.y);
        }

        // Ripristina la politica attiva in base al nome salvato
        PolicyStrategy policy = switch (data.activePolicy) {
            case "AusterityPolicy"  -> new AusterityPolicy();
            case "FossilFuelPolicy" -> new FossilFuelPolicy();
            case "GreenPolicy"      -> new GreenPolicy();
            default                 -> new DefaultPolicy();
        };
        city.setPolicy(policy);

        return data.tick;
    }

    /**
     * Costruisce il DTO SaveData a partire dallo stato corrente della città.
     *
     * Per ogni struttura decorata, risale la catena di Decorator per recuperare
     * il tipo base e raccogliere la lista ordinata degli upgrade tramite collectUpgrades().
     *
     * @param city la città di cui serializzare lo stato
     * @param tick il numero di tick corrente
     * @return un SaveData pronto per la serializzazione JSON
     */
    private SaveData buildSaveData(City city, int tick) {
        SaveData data = new SaveData();
        data.tick = tick;
        data.activePolicy = city.getActivePolicy().getClass().getSimpleName();

        CityState s = city.getState();
        data.budget     = s.getBudget();
        data.population = s.getPopulation();
        data.pollution  = s.getPollution();
        data.happiness  = s.getHappiness();
        data.health     = s.getHealth();
        data.wasteLevel = s.getWasteLevel();

        data.jobSatisfaction    = s.getPopulationGroup().getJobSatisfaction();
        data.healthSatisfaction = s.getPopulationGroup().getHealthSatisfaction();
        data.safetySatisfaction = s.getPopulationGroup().getSafetySatisfaction();
        data.totalProduction    = city.getPowerNet().getTotalProduction();
        data.totalConsumption   = city.getPowerNet().getTotalConsumption();

        Grid grid = city.getGrid();
        data.buildings = new ArrayList<>();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (!cell.isEmpty() && cell.getStructure() instanceof Structure st) {
                    BuildingEntry entry = new BuildingEntry();
                    entry.x = x;
                    entry.y = y;
                    if (st instanceof StructureDecorator dec) {
                        // Raccoglie la lista degli upgrade e risale al tipo base
                        entry.upgrades = dec.collectUpgrades();
                        Structure base = st;
                        while (base instanceof StructureDecorator dd) base = dd.wrapped;
                        entry.type = base.getType().name();
                        entry.hp   = st.getHp();
                    } else {
                        entry.type    = st.getType().name();
                        entry.hp      = st.getHp();
                        entry.upgrades = Collections.emptyList();
                    }
                    data.buildings.add(entry);
                }
            }
        }
        return data;
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    /**
     * DTO radice del file di salvataggio: contiene tutte le metriche della città,
     * il tick corrente, la politica attiva e la lista degli edifici sulla griglia.
     */
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

    /**
     * DTO per un singolo edificio: tipo, coordinate sulla griglia e lista degli upgrade applicati.
     * La lista upgrades è ordinata dal più interno al più esterno (ordine di applicazione al caricamento).
     */
    public static class BuildingEntry {
        public String       type;
        public int          x;
        public int          y;
        public int          hp = -1; // -1 indica che l'HP non era presente nel file JSON (vecchi save)
        public List<String> upgrades = new ArrayList<>();
    }
}
