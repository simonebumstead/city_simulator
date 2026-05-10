package it.citylife.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class GameController {

    private City city;
    private final SaveLoadManager ioManager = new SaveLoadManager();

    public GameController() {
        this.city = new City();
    }

    public void advanceTick() {
        city.advanceTick();
    }

    public boolean placeBuilding(String type, int x, int y) {
        Structure building = BuildingFactory.createBuilding(type);
        Cell cell = city.getGrid().getCell(x, y);

        if (cell == null || !cell.isEmpty()) {
            System.out.println("Cella occupata o non valida.");
            return false;
        }
        if (city.getState().getBudget() < building.getConstructionCost()) {
            System.out.println("Budget insufficiente per costruire: " + type);
            return false;
        }

        city.getGrid().placeStructure(building, x, y);
        city.getState().updateBudget(-building.getConstructionCost());
        System.out.println("[BUILD] Piazzato " + type + " in (" + x + "," + y + ") | Costo: " + building.getConstructionCost() + " | Budget rimasto: " + city.getState().getBudget());
        city.notifyObserversPublic();
        return true;
    }

    public void changePolicy(PolicyStrategy policy) {
        city.setPolicy(policy);
        if (policy == null) {
            city.setPolicy(new DefaultPolicy());
            System.out.println("[POLICY] Policy deselezionata. Ripristinata DefaultPolicy neutrale.");
        } else {
            city.setPolicy(policy);
            System.out.println("[POLICY] Policy cambiata in: " + policy.getClass().getSimpleName());
        }
    }

    public void addObserver(StateObserver o) {
        city.addObserver(o);
    }

    public boolean demolish(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) return false;

        if (cell.getStructure() instanceof Structure s) {
            int refund = s.getConstructionCost() / 2;
            city.getState().updateBudget(refund);
            System.out.println("[DEMOLISH] Rimborsati " + refund + " | Budget: " + city.getState().getBudget());
        }

        city.getGrid().removeStructure(x, y);
        city.notifyObserversPublic();
        return true;
    }

    public Path saveGame(int tick) throws IOException {
        return ioManager.saveAuto(city, tick);
    }

    public List<Path> listSaves() throws IOException {
        return ioManager.listSaves();
    }

    public int loadGame(Path path) throws IOException {
        int tick = ioManager.load(city, path);
        city.notifyObserversPublic();
        return tick;
    }

    public CityState getState() { return city.getState(); }
    public Grid getGrid() { return city.getGrid(); }
    public PowerNetwork getPowerNet() { return city.getPowerNet(); }
}
