package it.citylife.model;

import it.citylife.model.SaveLoadManager.BuildingEntry;
import it.citylife.model.SaveLoadManager.SaveData;

/**
 * Applica un {@link SaveData} deserializzato a una {@link City} esistente:
 * azzera la griglia, ripristina metriche e rete elettrica, ricostruisce ogni
 * struttura tramite {@link BuildingFactory} riapplicando gli upgrade nell'ordine
 * salvato per ricreare la catena di Decorator.
 */
final class SaveDataApplier {

    private SaveDataApplier() {}

    static void apply(City city, SaveData data) {
        city.clearDisasterObservers();
        clearGrid(city.getGrid());
        restoreMetrics(city.getState(), data);
        restorePower(city.getPowerNet(), data);
        restoreBuildings(city, data);
        city.setPolicy(policyFromName(data.activePolicy));
    }

    private static void clearGrid(Grid grid) {
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                grid.removeStructure(x, y);
            }
        }
    }

    private static void restoreMetrics(CityState s, SaveData data) {
        s.setBudget(data.budget);
        s.setPopulation(data.population);
        s.setHappiness(data.happiness);
        s.setHealth(data.health);
        s.setPollution(data.pollution);
        s.setWasteLevel(data.wasteLevel);

        s.getPopulationGroup().setJobSatisfaction(data.jobSatisfaction);
        s.getPopulationGroup().setHealthSatisfaction(data.healthSatisfaction);
        s.getPopulationGroup().setSafetySatisfaction(data.safetySatisfaction);
    }

    private static void restorePower(PowerNetwork net, SaveData data) {
        net.reset();
        net.addProduction(data.totalProduction);
        net.addConsumption(data.totalConsumption);
    }

    private static void restoreBuildings(City city, SaveData data) {
        Grid grid = city.getGrid();
        for (BuildingEntry entry : data.buildings) {
            Structure building = BuildingFactory.createBuilding(entry.type);

            // Ripristina HP se presenti (entry.hp = -1 per save legacy senza HP)
            if (entry.hp >= 0) {
                int damage = building.getMaxHp() - entry.hp;
                if (damage > 0) building.takeDamage(damage);
            }

            if (entry.upgrades != null) {
                for (String upgrade : entry.upgrades) {
                    building = BuildingFactory.applyUpgrade(building, upgrade);
                }
            }
            grid.placeStructure(building, entry.x, entry.y);
            city.addDisasterObserver(building);
        }
    }

    private static PolicyStrategy policyFromName(String name) {
        return switch (name) {
            case "AusterityPolicy"  -> new AusterityPolicy();
            case "FossilFuelPolicy" -> new FossilFuelPolicy();
            case "GreenPolicy"      -> new GreenPolicy();
            default                 -> new DefaultPolicy();
        };
    }
}
