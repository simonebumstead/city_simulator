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

    public City() {
        this.grid = new Grid();
        this.state = new CityState();
        this.powerNet = new PowerNetwork();
        this.observers = new ArrayList<>();
        this.activePolicy = new GreenPolicy();

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
        // 1. Reset della rete elettrica
        powerNet.reset();

        // 2. Itera la griglia e applica gli effetti di ogni struttura
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s) {
                    s.applyEffects(state, powerNet);
                }
            }
        }

        // 3. Applica i modificatori della policy attiva
        PolicyModifiers mod = activePolicy.getModifiers();
        state.updateHappiness(state.getHappiness() * mod.getHappinessMultiplier() - state.getHappiness());
        state.updateHealth(state.getHealth() * mod.getHealthMultiplier() - state.getHealth());
        state.updatePollution(state.getPollution() * mod.getPollutionMultiplier() - state.getPollution());
        state.setWasteLevel((int)(state.getWasteLevel() * mod.getWasteMultiplier()));
        state.updateBudget(mod.getFixedBudgetChange());
        if (mod.getFixedHealthChange() != 0) {
            state.updateHealth(mod.getFixedHealthChange());
        }

        // 4. Aggiorna la popolazione
        new PopulationManager().updateDemographics(state);

        // Evento casuale: terremoto con probabilità 7%
        if (random.nextDouble() < 0.07) {
            disasterManager.triggerEarthquake(grid, state);
        }

        System.out.println("=== TICK ===");
        System.out.printf("  Budget: %.0f | Pop: %d | Happiness: %.1f | Health: %.1f | Pollution: %.1f | Waste: %d%n",
            state.getBudget(), state.getPopulation(), state.getHappiness(),
            state.getHealth(), state.getPollution(), state.getWasteLevel());
        System.out.println("  Policy: " + activePolicy.getClass().getSimpleName());
        System.out.println("  Power: " + (powerNet.hasEnoughPower() ? "OK" : "BLACKOUT"));
    }

    // Metodo per registrare la UI come osservatore
    public void addObserver(StateObserver observer) {
        this.observers.add(observer);
    }

    private void notifyObservers() {
        for (StateObserver obs : observers) {
            obs.onStateChanged(this.state);
        }
    }

    public void notifyObserversPublic() {
        notifyObservers();
    }

    public void setPolicy(PolicyStrategy policy) {
        this.activePolicy = policy;
        System.out.println("[POLICY] Cambiata a: " + policy.getClass().getSimpleName());
    }
    public PolicyStrategy getActivePolicy() { return activePolicy; }

    // Getters per permettere alla UI di leggere (ma non modificare direttamente)
    public CityState getState() { return state; }
    public Grid getGrid() { return grid; }
    public PowerNetwork getPowerNet() { return powerNet; }
}