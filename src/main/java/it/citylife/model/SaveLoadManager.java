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

public class SaveLoadManager {

    static final Path SAVES_DIR = Path.of(System.getProperty("user.dir"), "saves");
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ObjectMapper mapper = new ObjectMapper();

    public Path saveAuto(City city, int tick) throws IOException {
        Files.createDirectories(SAVES_DIR);
        String filename = "save_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".json";
        Path file = SAVES_DIR.resolve(filename);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), buildSaveData(city, tick));
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

        Grid grid = city.getGrid();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                grid.removeStructure(x, y);
            }
        }

        CityState s = city.getState();
        s.setBudget(data.budget);
        s.setPopulation(data.population);
        s.setHappiness(data.happiness);
        s.setHealth(data.health);
        s.setPollution(data.pollution);
        s.setWasteLevel(data.wasteLevel);

        for (BuildingEntry entry : data.buildings) {
            Structure building = BuildingFactory.createBuilding(entry.type);
            if (entry.upgrades != null) {
                for (String upgrade : entry.upgrades) {
                    building = BuildingFactory.applyUpgrade(building, upgrade);
                }
            }
            grid.placeStructure(building, entry.x, entry.y);
        }

        PolicyStrategy policy = switch (data.activePolicy) {
            case "AusterityPolicy"  -> new AusterityPolicy();
            case "FossilFuelPolicy" -> new FossilFuelPolicy();
            default                 -> new GreenPolicy();
        };
        city.setPolicy(policy);

        return data.tick;
    }

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
                        entry.upgrades = dec.collectUpgrades();
                        Structure base = st;
                        while (base instanceof StructureDecorator dd) base = dd.wrapped;
                        entry.type = base.getType().name();
                    } else {
                        entry.type = st.getType().name();
                        entry.upgrades = Collections.emptyList();
                    }
                    data.buildings.add(entry);
                }
            }
        }
        return data;
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public static class SaveData {
        public int tick;
        public String activePolicy;
        public double budget;
        public int population;
        public double pollution;
        public double happiness;
        public double health;
        public int wasteLevel;
        public List<BuildingEntry> buildings;
    }

    public static class BuildingEntry {
        public String type;
        public int x;
        public int y;
        public List<String> upgrades = new ArrayList<>();
    }
}
