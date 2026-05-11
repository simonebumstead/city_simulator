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
        Grid grid = city.getGrid();
        
        // Pre-pass: aggiorna lo stato (strade e corrente) di tutti gli edifici 
        // prima che City.java applichi gli effetti.
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s) {
                    s.setConnectedToRoad(hasAdjacentRoad(x, y));
                    s.setPowered(isPowered(x, y));
                }
            }
        }
        
        city.advanceTick();
    }

    private boolean isPowered(int x, int y) {
        Grid grid = city.getGrid();
        for (int px = 0; px < grid.getWidth(); px++) {
            for (int py = 0; py < grid.getHeight(); py++) {
                Cell pc = grid.getCell(px, py);
                if (pc != null && pc.getStructure() instanceof PowerPlant) {
                    if (Math.max(Math.abs(px - x), Math.abs(py - y)) <= 5) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasAdjacentRoad(int x, int y) {
        Grid grid = city.getGrid();
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && nx < grid.getWidth() && ny >= 0 && ny < grid.getHeight()) {
                Cell c = grid.getCell(nx, ny);
                if (c != null && c.getStructure() != null && c.getStructure().getType() == StructureType.ROAD) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean placeBuilding(String type, int x, int y) {
        Structure building = BuildingFactory.createBuilding(type);
        Cell cell = city.getGrid().getCell(x, y);

        if (cell == null || !cell.isEmpty()) {
            System.out.println("Cella occupata o non valida.");
            return false;
        }
        
        // 1. Blocco placement residenziale senza road
        if (building.getType() == StructureType.RESIDENTIAL && !hasAdjacentRoad(x, y)) {
            System.out.println("Devi costruire vicino a una strada!");
            return false;
        }

        if (city.getState().getBudget() < building.getConstructionCost()) {
            System.out.println("Budget insufficiente per costruire: " + type);
            return false;
        }

        city.getGrid().placeStructure(building, x, y);
        // Usa setBudget invece di updateBudget per modifiche immediate (non a fine turno)
        city.getState().setBudget(city.getState().getBudget() - building.getConstructionCost());
        
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
            int demolitionCost = s.getConstructionCost() / 10;
            if (city.getState().getBudget() < demolitionCost) {
                System.out.println("[DEMOLISH] Budget insufficiente per demolire! Costo: " + demolitionCost);
                return false;
            }
            int refund = s.getConstructionCost() / 2;
            city.getState().setBudget(city.getState().getBudget() + refund - demolitionCost);
            System.out.println("[DEMOLISH] Costo rimozione: " + demolitionCost + " | Rimborso: " + refund + " | Budget: " + city.getState().getBudget());
        }

        city.getGrid().removeStructure(x, y);
        city.notifyObserversPublic();
        return true;
    }
    
    public boolean repair(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) return false;

        if (cell.getStructure() instanceof Structure s) {
            if (s.isDestroyed() || s.getHp() == s.getMaxHp()) return false;
            
            int repairCost = (s.getMaxHp() - s.getHp()) * 2; 
            if (city.getState().getBudget() < repairCost) {
                System.out.println("Budget insufficiente per riparare!");
                return false;
            }
            
            s.fullRepair();
            // Usa setBudget per pagamenti immediati
            city.getState().setBudget(city.getState().getBudget() - repairCost);

            System.out.println("[REPAIR] Riparato per " + repairCost + " | Budget: " + city.getState().getBudget());
            city.notifyObserversPublic();
            return true;
        }
        return false;
    }

    public boolean upgradeBuilding(int x, int y, String upgradeType) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) return false;
        if (!(cell.getStructure() instanceof Structure base)) return false;

        // AC-16.3: max 3 upgrade annidati
        int currentLevel = (base instanceof StructureDecorator d) ? d.getUpgradeLevel() : 0;
        if (currentLevel >= 3) {
            System.out.println("[UPGRADE] Livello massimo upgrade raggiunto.");
            return false;
        }

        int cost = switch (upgradeType) {
            case "SEISMIC"       -> SeismicUpgrade.COST;
            case "WASTE_THERMAL" -> WasteThermalUpgrade.COST;
            default -> { System.out.println("[UPGRADE] Tipo sconosciuto: " + upgradeType); yield -1; }
        };
        if (cost < 0) return false;

        if (city.getState().getBudget() < cost) {
            System.out.println("[UPGRADE] Budget insufficiente. Costo: " + cost);
            return false;
        }

        Structure upgraded = switch (upgradeType) {
            case "SEISMIC"       -> new SeismicUpgrade(base);
            case "WASTE_THERMAL" -> new WasteThermalUpgrade(base);
            default -> null;
        };
        if (upgraded == null) return false;

        city.getState().setBudget(city.getState().getBudget() - cost);
        cell.setStructure(upgraded);
        System.out.println("[UPGRADE] Applicato " + upgradeType + " in (" + x + "," + y + ") | Costo: " + cost);
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
