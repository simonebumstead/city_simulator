package it.citylife.model.core;

import it.citylife.model.disasters.DisasterManager;
import it.citylife.model.disasters.DisasterObserver;
import it.citylife.model.grid.Cell;
import it.citylife.model.grid.Grid;
import it.citylife.model.grid.GridQueries;
import it.citylife.model.policies.DefaultPolicy;
import it.citylife.model.policies.PolicyStrategy;
import it.citylife.model.structures.ResidentialBuilding;
import it.citylife.model.structures.Road;
import it.citylife.model.structures.Structure;
import it.citylife.model.structures.StructureType;
import it.citylife.model.structures.upgrades.StateObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Nucleo del motore di simulazione della città. 
 *
 * Coordina tutti i sottosistemi per tick: aggiornamento dello stato,
 * applicazione degli effetti degli edifici, gestione dei parchi, risoluzione
 * della politica attiva, aggiornamento demografico ed eventi casuali (terremoti).
 *
 * Implementa il Pattern Observer: al termine di ogni tick notifica tutti i
 * {@link StateObserver} registrati (tipicamente {@code DashboardView}) con
 * lo stato aggiornato.
 *
 * Il flusso di un tick è orchestrato da advanceTick(), che delega a updateState()
 * per tutta la logica di dominio e poi chiama notifyObservers().
 *
 * @see CityState
 * @see Grid
 * @see GameController
 * @see StateObserver
 */
public class City {

    // La mappa 20×20 che contiene tutte le celle e le strutture piazzate
    private Grid grid;

    // Tutti i contatori numerici della città (budget, popolazione, felicità, ecc.)
    private CityState state;

    // Traccia produzione e consumo energetico per il tick corrente
    private PowerNetwork powerNet;

    // Lista degli osservatori notificati al termine di ogni tick (Pattern Observer)
    private List<StateObserver> observers;

    // Politica economica attiva; determina i modificatori applicati in resolveTick()
    private PolicyStrategy activePolicy;

    // Gestore degli eventi sismici; incapsula la logica dei terremoti
    private final DisasterManager disasterManager = new DisasterManager();

    // Generatore casuale usato per la probabilità del terremoto
    private final Random random = new Random();

    // Raggio (distanza di Chebyshev) entro cui un Park applica il bonus happiness ai Residential
    private static final int    PARK_HAPPINESS_RADIUS    = 3;

    // Bonus di felicità aggiunto ogni tick a ogni Residential entro il raggio di un Park
    private static final double PARK_HAPPINESS_BONUS     = 2.0;

    // Riduzione di inquinamento applicata ogni tick da ogni Park (AC-05.4)
    private static final double PARK_POLLUTION_REDUCTION = 3.0;

    /**
     * Inizializza la città con una griglia vuota, lo stato di default e la politica neutrale.
     *
     * Il blocco commentato al suo interno era un popolamento casuale iniziale usato
     * durante lo sviluppo; è stato disabilitato perché il piazzamento degli edifici
     * è ora responsabilità esclusiva del giocatore tramite GameController.
     */
    public City() {
        this.grid = new Grid();
        this.state = new CityState();
        this.powerNet = new PowerNetwork();
        this.observers = new ArrayList<>();

        // Impostiamo la politica neutrale di default alla partenza del gioco
        this.activePolicy = new DefaultPolicy();
    }

    /**
     * Ripristina la città allo stato iniziale (nuova partita), mantenendo intatti gli osservatori registrati.
     */
    public void reset() {
        this.grid = new Grid();
        this.state = new CityState();
        this.powerNet = new PowerNetwork();
        this.activePolicy = new DefaultPolicy();
        this.disasterManager.clearObservers();
        notifyObserversPublic();
    }

    // --- METODI CORE ---

    /**
     * Avanza la simulazione di un tick.
     *
     * Esegue in ordine:
     *   1. updateState(): aggiorna tutti i sottosistemi e risolve i delta
     *   2. notifyObservers(): notifica la UI con lo stato aggiornato
     *
     * Chiamato da GameController.advanceTick(), che prima di invocare questo metodo
     * esegue un pre-pass sulla griglia per impostare i flag connectedToRoad e powered.
     *
     * @see GameController#advanceTick()
     */
    public void advanceTick() {
        // 1. Aggiorna lo stato interno (Information Expert)
        updateState();

        // 2. Notifica la UI che i dati sono cambiati (Observer Pattern)
        notifyObservers();
    }

    private void updateState() {
        tickResetPhase();
        BuildingCounts counts = tickStructuresPhase();
        cleanupDecayedBuildings();
        applyParkEffects();
        int maxCapacity = tickCapacityPhase(counts.residential);
        state.resolveTick(activePolicy.getModifiers());
        tickDisastersPhase();
        tickDemographicsPhase(counts, maxCapacity);
        logTickSummary(maxCapacity);
    }

    private void tickResetPhase() {
        state.setEarthquakeOccurred(false);
        state.resetCriticalBuildings();
        state.setOverpopulated(false);
        powerNet.reset();
    }

    private BuildingCounts tickStructuresPhase() {
        BuildingCounts c = new BuildingCounts();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell == null || !(cell.getStructure() instanceof Structure s)) continue;
                processStructure(s, c);
            }
        }
        return c;
    }

    private void processStructure(Structure s, BuildingCounts c) {
        // AC-15.1: ogni struttura decade di HP_DECAY_PER_TICK ogni tick
        s.decayTick();

        // AC-15.2: struttura critica se HP > 0 e inferiore al 20% del massimo
        if (s.getHp() > 0 && s.getHp() < s.getMaxHp() * 0.20) {
            state.incrementCriticalBuildings();
        }

        // AC-15.4: edifici a 0 HP non applicano effetti né generano risorse
        if (s.isDestroyed()) return;

        // Edifici che richiedono corrente ma non sono coperti da una PowerPlant non applicano
        // effetti; i Residential vengono comunque contati per la capacità.
        // Usa il flag aggiornato dalla pre-pass di GameController.advanceTick()
        if (!s.isPowered() && requiresPower(s)) {
            if (s.getType() == StructureType.RESIDENTIAL) c.residential++;
            return;
        }

        // Edifici senza strada adiacente non generano entrate di budget.
        // Usa il flag aggiornato dalla pre-pass di GameController.advanceTick()
        if (!s.isConnectedToRoad() && isRevenueBuilding(s)) {
            double budgetBefore = state.getDeltaBudget();
            s.applyEffects(state, powerNet);
            double budgetAdded = state.getDeltaBudget() - budgetBefore;
            if (budgetAdded > 0) state.updateBudget(-budgetAdded);
        } else {
            s.applyEffects(state, powerNet);
        }

        // Conteggio per tipo usando getType() per funzionare anche con i Decorator
        switch (s.getType()) {
            case RESIDENTIAL -> c.residential++;
            case INDUSTRIAL  -> c.industrial++;
            case COMMERCIAL  -> c.commercial++;
            case HOSPITAL    -> c.hospital++;
            default -> {}
        }
    }

    private int tickCapacityPhase(int residentialCount) {
        int maxCapacity = residentialCount * 200;
        if (state.getPopulation() > maxCapacity) state.setOverpopulated(true);
        return maxCapacity;
    }

    private void tickDisastersPhase() {
        if (random.nextDouble() < DisasterManager.EARTHQUAKE_PROBABILITY) {
            disasterManager.triggerEarthquake(state);
            state.setEarthquakeOccurred(true);
            
            int collapsedBuildings = 0;
            for (int x = 0; x < grid.getWidth(); x++) {
                for (int y = 0; y < grid.getHeight(); y++) {
                    Cell c = grid.getCell(x, y);
                    if (c != null && c.getStructure() instanceof Structure s && s.isDestroyed()) {
                        grid.removeStructure(x, y);
                        disasterManager.removeObserver(s);
                        collapsedBuildings++;
                    }
                }
            }
            if (collapsedBuildings > 0) {
                // Se degli edifici sono crollati, è necessario aggiornare subito le connessioni
                // stradali in modo che la UI possa ridisegnarle correttamente nello stesso tick,
                // senza attendere il pre-pass del tick successivo.
                updateRoadConnections();
                System.out.println(String.format("%d buildings have collapsed.", collapsedBuildings));
            } else {
                System.out.println("Fortunately, no buildings collapsed.");
            }
        }
    }

    private void tickDemographicsPhase(BuildingCounts c, int maxCapacity) {
        boolean hasPowerNearby = anyResidentialHasPower();
        new PopulationManager().updateDemographics(state, hasPowerNearby, maxCapacity,
            c.industrial, c.commercial, c.hospital, c.residential);
    }

    private void logTickSummary(int maxCapacity) {
        System.out.println("=== TICK ===");
        System.out.printf("  Budget: %.0f | Pop: %d/%d | Happiness: %.1f | Health: %.1f | Pollution: %.1f | Waste: %d%n",
            state.getBudget(), state.getPopulation(), maxCapacity, state.getHappiness(),
            state.getHealth(), state.getPollution(), state.getWasteLevel());
        System.out.println("  Policy: " + activePolicy.getClass().getSimpleName());
        System.out.println("  Power: " + (powerNet.hasEnoughPower() ? "OK" : "BLACKOUT"));
    }

    /**
     * Scansiona la griglia alla ricerca di edifici distrutti dal decadimento naturale e li rimuove.
     *
     * Questo metodo viene eseguito dopo che il decadimento è stato applicato in `tickStructuresPhase`,
     * ma prima di qualsiasi altro evento come un terremoto. In questo modo, si assicura che gli
     * edifici che raggiungono 0 HP a causa del tempo vengano correttamente rimossi dalla mappa
     * e non continuino a essere renderizzati.
     */
    private void cleanupDecayedBuildings() {
        int collapsedCount = 0;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell c = grid.getCell(x, y);
                // Qualsiasi edificio distrutto in questa fase del tick è crollato per decadimento.
                if (c != null && c.getStructure() instanceof Structure s && s.isDestroyed()) {
                    grid.removeStructure(x, y);
                    disasterManager.removeObserver(s);
                    collapsedCount++;
                }
            }
        }
        if (collapsedCount > 0) {
            System.out.println(String.format("%d building(s) collapsed due to decay.", collapsedCount));
            updateRoadConnections();
        }
    }

    private static final class BuildingCounts {
        int residential, industrial, commercial, hospital;
    }

    /**
     * Verifica se almeno un edificio residenziale della griglia è alimentato.
     *
     * Usato da updateState() per determinare se la crescita demografica
     * può avvenire (la popolazione non cresce senza corrente nei Residential).
     *
     * @return true se esiste almeno un ResidentialBuilding coperto da una PowerPlant
     */
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

    /**
     * Verifica se la cella alle coordinate (x, y) è coperta da almeno una PowerPlant
     * entro distanza di Chebyshev {@link GridQueries#POWER_RADIUS}.
     */
    public boolean isPowered(int x, int y) {
        return GridQueries.isPoweredAt(grid, x, y);
    }

    private boolean hasAdjacentRoad(int x, int y) {
        return GridQueries.hasAdjacentRoad(grid, x, y);
    }

    /**
     * Aggiorna i flag di connessione stradale per tutte le strade presenti sulla griglia.
     * Una strada si connette a qualsiasi struttura adiacente (altre strade o edifici).
     */
    public void updateRoadConnections() {
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s) {
                    // Aggiorna istantaneamente i flag per i Tooltip della UI
                    s.setConnectedToRoad(hasAdjacentRoad(x, y));
                    s.setPowered(isPowered(x, y));

                    if (s.getBaseStructure() instanceof Road road) {
                        road.setConnectedNorth(canConnectRoad(x, y - 1));
                        road.setConnectedSouth(canConnectRoad(x, y + 1));
                        road.setConnectedEast(canConnectRoad(x + 1, y));
                        road.setConnectedWest(canConnectRoad(x - 1, y));
                    }
                }
            }
        }
    }

    private boolean canConnectRoad(int x, int y) {
        if (x < 0 || x >= grid.getWidth() || y < 0 || y >= grid.getHeight()) return false;
        Cell c = grid.getCell(x, y);
        return c != null && !c.isEmpty();
    }

    /**
     * Indica se la struttura richiede alimentazione elettrica per funzionare.
     *
     * Residential, Commercial e Industrial sono i tipi che non applicano effetti
     * se non coperti da una PowerPlant.
     *
     * @param s la struttura da verificare
     * @return true se la struttura richiede corrente
     */
    private static boolean requiresPower(Structure s) {
        return switch (s.getType()) {
            case RESIDENTIAL, COMMERCIAL, INDUSTRIAL, HOSPITAL, WASTE_CENTER -> true;
            default -> false;
        };
    }

    /**
     * Indica se la struttura genera entrate di budget (revenue).
     *
     * @param s la struttura da verificare
     * @return true se la struttura produce revenue di budget
     */
    private static boolean isRevenueBuilding(Structure s) {
        return s.getType() == StructureType.COMMERCIAL || s.getType() == StructureType.INDUSTRIAL;
    }

    /**
     * Applica gli effetti di tutti i parchi presenti sulla griglia.
     *
     * Per ogni Park:
     *   - AC-05.4: riduce il deltaInquinamento globale di PARK_POLLUTION_REDUCTION (−3/tick)
     *   - AC-05.3: aggiunge PARK_HAPPINESS_BONUS (+2/tick) a ogni ResidentialBuilding
     *     entro una distanza di Chebyshev pari a PARK_HAPPINESS_RADIUS (3 celle)
     *
     * Chiamato dopo il loop principale degli edifici, prima di resolveTick(),
     * in modo che i delta dei parchi vengano inclusi nella risoluzione della politica.
     */
    private void applyParkEffects() {
        for (int px = 0; px < grid.getWidth(); px++) {
            for (int py = 0; py < grid.getHeight(); py++) {
                Cell parkCell = grid.getCell(px, py);
                if (parkCell == null || parkCell.isEmpty() || parkCell.getStructure().getType() != StructureType.PARK) continue;

                // AC-05.4: ogni Park riduce l'inquinamento globale
                state.updatePollution(-PARK_POLLUTION_REDUCTION);

                // AC-05.3: bonus happiness per ogni Residential entro raggio
                for (int rx = 0; rx < grid.getWidth(); rx++) {
                    for (int ry = 0; ry < grid.getHeight(); ry++) {
                        Cell resCell = grid.getCell(rx, ry);
                        if (resCell == null || resCell.isEmpty() || resCell.getStructure().getType() != StructureType.RESIDENTIAL) continue;
                        if (Math.max(Math.abs(px - rx), Math.abs(py - ry)) <= PARK_HAPPINESS_RADIUS) {
                            state.updateHappiness(PARK_HAPPINESS_BONUS);
                        }
                    }
                }
            }
        }
    }

    /** Registra un osservatore che verrà notificato al termine di ogni tick. */
    public void addObserver(StateObserver observer) { this.observers.add(observer); }

    /** Notifica tutti gli osservatori registrati con lo stato corrente. */
    private void notifyObservers() { for (StateObserver obs : observers) { obs.onStateChanged(this.state); } }

    /**
     * Versione pubblica di notifyObservers(); usata da GameController dopo operazioni
     * (piazzamento, demolizione, riparazione) che modificano lo stato senza avanzare il tick.
     */
    public void notifyObserversPublic() { notifyObservers(); }

    /** Imposta la politica economica attiva; ha effetto dal tick successivo. */
    public void setPolicy(PolicyStrategy policy) { this.activePolicy = policy; System.out.println("[POLICY] Changed to: " + policy.getClass().getSimpleName()); }

    /** Restituisce la politica economica attualmente attiva. */
    public PolicyStrategy getActivePolicy() { return activePolicy; }

    /** Restituisce lo stato corrente della città (metriche, popolazione, ecc.). */
    public CityState getState() { return state; }

    /** Restituisce la griglia della città. */
    public Grid getGrid() { return grid; }

    /** Restituisce la rete elettrica corrente. */
    public PowerNetwork getPowerNet() { return powerNet; }

    public void addDisasterObserver(DisasterObserver obs) {
        disasterManager.addObserver(obs);
    }

    public void removeDisasterObserver(DisasterObserver obs) {
        disasterManager.removeObserver(obs);
    }

    public void clearDisasterObservers() {
        disasterManager.clearObservers();
    }
}
