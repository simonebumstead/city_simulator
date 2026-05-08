package it.citylife.model;

public class GameController {

    private City city;
    // private SaveLoadManager ioManager; // da implementare

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
    }

    public void addObserver(StateObserver o) {
        city.addObserver(o);
    }

    public boolean demolish(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) return false;
        city.getGrid().removeStructure(x, y);
        return true;
    }

    public CityState getState() { return city.getState(); }
    public Grid getGrid() { return city.getGrid(); }
    public PowerNetwork getPowerNet() { return city.getPowerNet(); }
}
