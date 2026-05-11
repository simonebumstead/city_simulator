package it.citylife.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

public class City {
    // Componenti principali definiti nel Domain Model
    private Grid grid;               // La mappa 20x20
    private CityState state;         // I contatori (budget, pop, ecc.)
    private PowerNetwork powerNet;   // La gestione dell'energia

    // Questa è una lista che contiene tutti i "soggetti" interessati ai cambiamenti della città.
    // Usiamo l'interfaccia 'StateObserver' invece di una classe specifica (es. DashboardUI)
    // per mantenere il codice flessibile (Disaccoppiamento).
    private List<StateObserver> observers;
    private PolicyStrategy activePolicy;
    private final DisasterManager disasterManager = new DisasterManager();
    private final Random random = new Random();

    private static final int    PARK_HAPPINESS_RADIUS    = 3;
    private static final double PARK_HAPPINESS_BONUS     = 2.0;
    private static final double PARK_POLLUTION_REDUCTION = 3.0;

    public City() {
        this.grid = new Grid();
        this.state = new CityState();
        this.powerNet = new PowerNetwork();
        this.observers = new ArrayList<>();

        // Impostiamo la politica neutrale di default alla partenza del gioco
        this.activePolicy = new DefaultPolicy();


        /*
        // Edifici di default in posizioni casuali non sovrapposte
        List<Structure> defaults = Arrays.asList(
            new PowerPlant(),
            new ResidentialBuilding(), new ResidentialBuilding(), new ResidentialBuilding(),
            new IndustrialBuilding(), new IndustrialBuilding(),
            new CommercialBuilding(),
            new Park()
        );
        for (Structure s : defaults) {
            int rx, ry;
            do {
                rx = random.nextInt(20);
                ry = random.nextInt(20);
            } while (!grid.getCell(rx, ry).isEmpty());
            grid.placeStructure(s, rx, ry);
        }
         */
    }

    // --- METODI CORE  ---

    //Coordina l'aggiornamento di tutti i sistemi.
    public void advanceTick() {
        // 1. Aggiorna lo stato interno (Information Expert)
        updateState();



        // 2. Notifica la UI che i dati sono cambiati (Observer Pattern)
        notifyObservers();
    }

    private void updateState() {
        // 0. Reset flag per-tick
        state.setEarthquakeOccurred(false);
        state.resetCriticalBuildings();

        // 1. Reset della rete elettrica
        powerNet.reset();

        // Variabile per contare la capacità abitativa e i gruppi edilizi per AC-19
        int residentialCount = 0;
        int industrialCount  = 0;
        int commercialCount  = 0;
        int hospitalCount    = 0;

        // 2. Itera la griglia e applica gli effetti di ogni struttura
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s) {

                    // AC-15.1: ogni struttura decade di HP_DECAY_PER_TICK ogni tick
                    s.decayTick();

                    // AC-15.2: conta edifici in stato critico (HP < 20% maxHp)
                    if (s.getHp() > 0 && s.getHp() < s.getMaxHp() * 0.20) {
                        state.incrementCriticalBuildings();
                    }

                    // AC-15.4: edifici distrutti non applicano effetti
                    if (s.isDestroyed()) continue;

                    // 2. Edifici non alimentati non applicano effetti
                    boolean powered = isPowered(x, y);
                    if (!powered && requiresPower(s)) {
                        // Salta l'applicazione degli effetti, ma conta comunque la struttura se è residenziale
                        if (s.getType() == StructureType.RESIDENTIAL) residentialCount++;
                        continue;
                    }

                    // 3. Edifici isolati non generano revenue
                    boolean isolated = !hasAdjacentRoad(x, y);
                    if (isolated && isRevenueBuilding(s)) {
                        // Applica effetti ma annulla la revenue
                        double budgetBefore = state.getDeltaBudget();
                        s.applyEffects(state, powerNet);
                        double budgetAfter = state.getDeltaBudget();
                        double budgetAdded = budgetAfter - budgetBefore;
                        if (budgetAdded > 0) {
                            state.updateBudget(-budgetAdded); // Annulla il guadagno
                        }
                    } else {
                        s.applyEffects(state, powerNet);
                    }
                    
                    // Conteggio edifici per AC-19 (usa getType() per funzionare anche con i Decorator)
                    switch (s.getType()) {
                        case RESIDENTIAL -> residentialCount++;
                        case INDUSTRIAL  -> industrialCount++;
                        case COMMERCIAL  -> commercialCount++;
                        case HOSPITAL    -> hospitalCount++;
                        default -> {}
                    }
                }
            }
        }

        applyParkEffects();

        PolicyModifiers mod = activePolicy.getModifiers();
        state.resolveTick(mod);

        // 4. Aggiorna la popolazione (Crescita e Sovrappopolazione)
        boolean hasPowerNearby = anyResidentialHasPower();
        int maxCapacity = residentialCount * 200; // 200 abitanti per ogni area residenziale
        new PopulationManager().updateDemographics(state, hasPowerNearby, maxCapacity,
            industrialCount, commercialCount, hospitalCount, residentialCount);

        // Evento casuale: terremoto con probabilità 1%
        if (random.nextDouble() < DisasterManager.EARTHQUAKE_PROBABILITY) {
            disasterManager.triggerEarthquake(grid, state);
            state.setEarthquakeOccurred(true);
        }

        System.out.println("=== TICK ===");
        System.out.printf("  Budget: %.0f | Pop: %d/%d | Happiness: %.1f | Health: %.1f | Pollution: %.1f | Waste: %d%n",
            state.getBudget(), state.getPopulation(), maxCapacity, state.getHappiness(),
            state.getHealth(), state.getPollution(), state.getWasteLevel());
        System.out.println("  Policy: " + activePolicy.getClass().getSimpleName());
        System.out.println("  Power: " + (powerNet.hasEnoughPower() ? "OK" : "BLACKOUT"));
    }

    private boolean anyResidentialHasPower() {
        for (int rx = 0; rx < grid.getWidth(); rx++) {
            for (int ry = 0; ry < grid.getHeight(); ry++) {
                Cell rc = grid.getCell(rx, ry);
                if (rc != null && rc.getStructure() instanceof ResidentialBuilding) {
                    if (isPowered(rx, ry)) return true;
                }
            }
        }
        return false;
    }

    public boolean isPowered(int x, int y) {
        for (int px = 0; px < grid.getWidth(); px++) {
            for (int py = 0; py < grid.getHeight(); py++) {
                Cell pc = grid.getCell(px, py);
                if (pc != null && pc.getStructure() instanceof PowerPlant) {
                    if (Math.max(Math.abs(px - x), Math.abs(py - y)) <= 5)
                        return true;
                }
            }
        }
        return false;
    }

    private boolean hasAdjacentRoad(int x, int y) {
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

    private boolean requiresPower(Structure s) {
        return s instanceof ResidentialBuilding || s instanceof CommercialBuilding || s instanceof IndustrialBuilding;
    }

    private boolean isRevenueBuilding(Structure s) {
        return s instanceof CommercialBuilding || s instanceof IndustrialBuilding;
    }

    private void applyParkEffects() {
        for (int px = 0; px < grid.getWidth(); px++) {
            for (int py = 0; py < grid.getHeight(); py++) {
                Cell parkCell = grid.getCell(px, py);
                if (parkCell == null || !(parkCell.getStructure() instanceof Park)) continue;

                // AC-05.4: ogni Park riduce l'inquinamento globale
                state.updatePollution(-PARK_POLLUTION_REDUCTION);

                // AC-05.3: bonus happiness per ogni Residential entro raggio
                for (int rx = 0; rx < grid.getWidth(); rx++) {
                    for (int ry = 0; ry < grid.getHeight(); ry++) {
                        Cell resCell = grid.getCell(rx, ry);
                        if (resCell == null || !(resCell.getStructure() instanceof ResidentialBuilding)) continue;
                        if (Math.max(Math.abs(px - rx), Math.abs(py - ry)) <= PARK_HAPPINESS_RADIUS) {
                            state.updateHappiness(PARK_HAPPINESS_BONUS);
                        }
                    }
                }
            }
        }
    }

    public void addObserver(StateObserver observer) { this.observers.add(observer); }
    private void notifyObservers() { for (StateObserver obs : observers) { obs.onStateChanged(this.state); } }
    public void notifyObserversPublic() { notifyObservers(); }
    public void setPolicy(PolicyStrategy policy) { this.activePolicy = policy; System.out.println("[POLICY] Cambiata a: " + policy.getClass().getSimpleName()); }
    public PolicyStrategy getActivePolicy() { return activePolicy; }
    public CityState getState() { return state; }
    public Grid getGrid() { return grid; }
    public PowerNetwork getPowerNet() { return powerNet; }
}
