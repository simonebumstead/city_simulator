package it.citylife.model;

import java.util.ArrayList;
import java.util.Collections;

import it.citylife.model.SaveLoadManager.BuildingEntry;
import it.citylife.model.SaveLoadManager.SaveData;

/**
 * Estrae lo stato corrente di una {@link City} in un {@link SaveData} pronto
 * per la serializzazione JSON. Per le strutture decorate risale la catena di
 * Decorator per ricostruire la lista ordinata degli upgrade.
 */
final class SaveDataMapper {

    private SaveDataMapper() {}

    static SaveData toSaveData(City city, int tick) {
        SaveData data = new SaveData();
        data.tick = tick;
        data.activePolicy = city.getActivePolicy().getClass().getSimpleName();

        copyMetrics(city.getState(), data);
        copyPower(city.getPowerNet(), data);
        data.buildings = collectBuildings(city.getGrid());
        return data;
    }

    private static void copyMetrics(CityState s, SaveData data) {
        data.budget     = s.getBudget();
        data.population = s.getPopulation();
        data.pollution  = s.getPollution();
        data.happiness  = s.getHappiness();
        data.health     = s.getHealth();
        data.wasteLevel = s.getWasteLevel();

        data.jobSatisfaction    = s.getPopulationGroup().getJobSatisfaction();
        data.healthSatisfaction = s.getPopulationGroup().getHealthSatisfaction();
        data.safetySatisfaction = s.getPopulationGroup().getSafetySatisfaction();
    }

    private static void copyPower(PowerNetwork net, SaveData data) {
        data.totalProduction  = net.getTotalProduction();
        data.totalConsumption = net.getTotalConsumption();
    }

    private static ArrayList<BuildingEntry> collectBuildings(Grid grid) {
        ArrayList<BuildingEntry> list = new ArrayList<>();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell.isEmpty() || !(cell.getStructure() instanceof Structure st)) continue;
                list.add(toEntry(st, x, y));
            }
        }
        return list;
    }

    private static BuildingEntry toEntry(Structure st, int x, int y) {
        BuildingEntry entry = new BuildingEntry();
        entry.x = x;
        entry.y = y;
        entry.hp = st.getHp();
        if (st instanceof StructureDecorator dec) {
            entry.upgrades = dec.collectUpgrades();
            Structure base = st;
            while (base instanceof StructureDecorator dd) base = dd.wrapped;
            entry.type = base.getType().name();
        } else {
            entry.type = st.getType().name();
            entry.upgrades = Collections.emptyList();
        }
        return entry;
    }
}
