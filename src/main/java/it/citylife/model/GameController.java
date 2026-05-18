package it.citylife.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Controller principale del gioco (GRASP Controller): fa da punto di ingresso
 * per tutte le operazioni che modificano lo stato della città.
 *
 * Riceve i comandi dalla UI tramite SimulationController e li traduce in
 * operazioni validate sul modello (City, Grid, CityState). Nessuna logica
 * di dominio vive qui: GameController si limita a validare le precondizioni
 * (budget sufficiente, cella libera, livello upgrade, ecc.) e a delegare
 * l'esecuzione agli oggetti di dominio appropriati.
 *
 * Operazioni gestite: avanzamento tick, piazzamento/demolizione/riparazione
 * edifici, upgrade delle strutture, cambio politica, salvataggio e caricamento.
 *
 * @see City
 * @see BuildingFactory
 * @see SaveLoadManager
 */
public class GameController {

    // Il modello principale della simulazione
    private City city;

    private String gameSessionId;

    // Gestore della persistenza: salvataggio e caricamento su file JSON
    private final SaveLoadManager ioManager = new SaveLoadManager();

    private String lastError = "";

    /**
     * Crea un nuovo GameController con una città vuota e una DefaultPolicy attiva.
     */
    public GameController() {
        this.city = new City();
        this.gameSessionId = String.valueOf(System.currentTimeMillis());
    }

    /**
     * Inizia una nuova partita resettando lo stato della città.
     */
    public void startNewGame() {
        city.reset();
        lastError = "";
        this.gameSessionId = String.valueOf(System.currentTimeMillis());
    }

    /**
     * Avanza la simulazione di un tick.
     *
     * Prima di delegare a City.advanceTick(), esegue un pre-pass sulla griglia
     * per aggiornare i flag connectedToRoad e powered su ogni struttura.
     * Questo garantisce che City.updateState() trovi i flag già aggiornati
     * quando applica gli effetti degli edifici.
     */
    public void advanceTick() {
        Grid grid = city.getGrid();

        // Pre-pass: aggiorna connectedToRoad e powered su ogni struttura prima
        // che City.advanceTick() applichi gli effetti — i flag devono essere freschi
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                Cell cell = grid.getCell(x, y);
                if (cell != null && cell.getStructure() instanceof Structure s) {
                    s.setConnectedToRoad(GridQueries.hasAdjacentRoad(grid, x, y));
                    s.setPowered(GridQueries.isPoweredAt(grid, x, y));
                }
            }
        }

        city.advanceTick();
    }

    /**
     * Piazza una nuova struttura del tipo indicato nella cella (x, y).
     *
     * Validazioni in ordine:
     *   1. La cella deve esistere ed essere vuota
     *   2. I Residential devono essere adiacenti a una Road
     *   3. Il budget deve coprire il costo di costruzione
     *
     * Se tutte le validazioni passano, la struttura viene piazzata e il budget
     * scalato immediatamente (setBudget, non updateBudget: modifica fuori dal tick).
     *
     * @param type stringa del tipo di struttura (es. "RESIDENTIAL", "POWER_PLANT")
     * @param x    colonna della cella target
     * @param y    riga della cella target
     * @return true se il piazzamento è andato a buon fine, false altrimenti
     */
    public boolean placeBuilding(String type, int x, int y) {
        Structure building = BuildingFactory.createBuilding(type);
        Cell cell = city.getGrid().getCell(x, y);

        // Cella occupata o fuori griglia: impossibile piazzare
        if (cell == null || !cell.isEmpty()) {
            lastError = "Cell occupied or invalid.";
            System.out.println("Cell occupied or invalid.");
            return false;
        }

        // I Residential richiedono strada adiacente: senza accesso i cittadini non possono abitarci
        if (building.getType() == StructureType.RESIDENTIAL && !GridQueries.hasAdjacentRoad(city.getGrid(), x, y)) {
            lastError = "Residential buildings must be built next to a Road!";
            System.out.println("Must build next to a road!");
            return false;
        }

        // Budget insufficiente: la costruzione non può avvenire
        if (city.getState().getBudget() < building.getConstructionCost()) {
            lastError = "Insufficient budget to build " + type + " (" + building.getConstructionCost() + "$).";
            System.out.println("Insufficient budget to build: " + type);
            return false;
        }

        city.getGrid().placeStructure(building, x, y);
        city.addDisasterObserver(building);
        // setBudget per modifica immediata: il costo di costruzione non transita nel delta di fine tick
        city.getState().setBudget(city.getState().getBudget() - building.getConstructionCost());

        city.updateRoadConnections();
        System.out.println("[BUILD] Placed " + type + " at (" + x + "," + y + ") | Cost: " + building.getConstructionCost() + " | Budget left: " + city.getState().getBudget());
        city.notifyObserversPublic();
        return true;
    }

    /**
     * Cambia la politica economica attiva della città.
     * Se policy è null, ripristina la DefaultPolicy neutrale.
     *
     * @param policy la nuova politica da applicare, oppure null per tornare al default
     */
    public void changePolicy(PolicyStrategy policy) {
        if (policy == null) {
            city.setPolicy(new DefaultPolicy());
            System.out.println("[POLICY] Policy deselected. Restored neutral DefaultPolicy.");
        } else {
            city.setPolicy(policy);
            System.out.println("[POLICY] Policy changed to: " + policy.getClass().getSimpleName());
        }
    }

    /**
     * Restituisce la politica attualmente attiva.
     * @return la politica attiva
     */
    public PolicyStrategy getActivePolicy() {
        return city.getActivePolicy();
    }

    /**
     * Registra un osservatore da notificare al termine di ogni tick.
     * Delega a City.addObserver().
     *
     * @param o l'osservatore da registrare (tipicamente DashboardView)
     */
    public void addObserver(StateObserver o) {
        city.addObserver(o);
    }

    /**
     * Demolisce la struttura nella cella (x, y) applicando costi e rimborso (AC-07.3).
     *
     * Costo demolizione:  10% del costo di costruzione della struttura.
     * Rimborso materiali: 50% del costo di costruzione.
     * Netto per il giocatore: +40% del costo di costruzione (rimborso − costo).
     *
     * Se il budget non copre il costo di demolizione, l'operazione viene annullata.
     *
     * @param x colonna della cella target
     * @param y riga della cella target
     * @return true se la demolizione è avvenuta, false se la cella è vuota o il budget è insufficiente
     */
    public boolean demolish(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) {
            lastError = "Nothing to demolish here.";
            return false;
        }

        if (cell.getStructure() instanceof Structure s) {
            // Costo demolizione = 10% del valore di costruzione (AC-07.3)
            int demolitionCost = s.getConstructionCost() / 10;
            if (city.getState().getBudget() < demolitionCost) {
                lastError = "Insufficient budget to demolish! Cost: " + demolitionCost + "$";
                System.out.println("[DEMOLISH] Insufficient budget to demolish! Cost: " + demolitionCost);
                return false;
            }
            // Rimborso = 50% del valore di costruzione; il netto è +40% per il giocatore
            int refund = s.getConstructionCost() / 2;
            city.getState().setBudget(city.getState().getBudget() + refund - demolitionCost);
            System.out.println("[DEMOLISH] Removal cost: " + demolitionCost + " | Refund: " + refund + " | Budget: " + city.getState().getBudget());
            city.removeDisasterObserver(s);
        }

        city.getGrid().removeStructure(x, y);
        city.updateRoadConnections();
        city.notifyObserversPublic();
        return true;
    }

    /**
     * Ripara completamente la struttura nella cella (x, y) (AC-15.3).
     *
     * Costo riparazione: (maxHp − hp correnti) × 2.
     * Se la struttura è distrutta (HP = 0) o già integra (HP = maxHp), l'operazione
     * viene rifiutata. Richiede budget sufficiente.
     *
     * @param x colonna della cella target
     * @param y riga della cella target
     * @return true se la riparazione è avvenuta, false altrimenti
     */
    public boolean repair(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) {
            lastError = "Nothing to repair here.";
            return false;
        }

        if (cell.getStructure() instanceof Structure s) {
            // Struttura distrutta o già a piena salute: niente da riparare
            if (s.isDestroyed()) {
                lastError = "Building is totally destroyed and cannot be repaired.";
                return false;
            }
            if (s.getHp() == s.getMaxHp()) {
                lastError = "Building is already at maximum HP.";
                return false;
            }

            // Costo proporzionale ai danni subiti: più è danneggiata, più costa (AC-15.3)
            int repairCost = (s.getMaxHp() - s.getHp()) / 2;  // Era *2, diminuito per rendere il decadimento degli edifici meno invasivo
            if (city.getState().getBudget() < repairCost) {
                lastError = "Insufficient budget to repair! Cost: " + repairCost + "$";
                System.out.println("Insufficient budget to repair!");
                return false;
            }

            s.fullRepair();
            // setBudget per pagamento immediato, fuori dal ciclo delta
            city.getState().setBudget(city.getState().getBudget() - repairCost);

            System.out.println("[REPAIR] Repaired for " + repairCost + " | Budget: " + city.getState().getBudget());
            city.notifyObserversPublic();
            return true;
        }
        return false;
    }

    /**
     * Applica un upgrade alla struttura nella cella (x, y) avvolgendola in un Decorator (AC-16.1).
     *
     * Validazioni in ordine:
     *   1. La cella deve contenere una struttura
     *   2. Il livello di upgrade corrente non deve superare 3 (AC-16.3)
     *   3. Il tipo di upgrade deve essere riconosciuto (SEISMIC o WASTE_THERMAL)
     *   4. Il budget deve coprire il costo dell'upgrade
     *
     * Se tutte le validazioni passano, la struttura viene sostituita nella cella
     * con la versione decorata e il budget scalato immediatamente.
     *
     * @param x           colonna della cella target
     * @param y           riga della cella target
     * @param upgradeType stringa del tipo di upgrade ("SEISMIC" o "WASTE_THERMAL")
     * @return true se l'upgrade è stato applicato, false altrimenti
     */
    public boolean upgradeBuilding(int x, int y, String upgradeType) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell == null || cell.isEmpty()) {
            lastError = "Nothing to upgrade here.";
            return false;
        }
        if (!(cell.getStructure() instanceof Structure base)) {
            lastError = "This cell does not contain a valid structure.";
            return false;
        }

        // AC-16.3: massimo 3 livelli di decorator annidati per struttura
        int currentLevel = (base instanceof StructureDecorator d) ? d.getUpgradeLevel() : 0;
        if (currentLevel >= 3) {
            lastError = "Maximum upgrade level (3) reached for this building.";
            System.out.println("[UPGRADE] Maximum upgrade level reached.");
            return false;
        }

        // Recupera il costo dalla costante statica del Decorator corrispondente
        int cost = switch (upgradeType) {
            case "SEISMIC"       -> SeismicUpgrade.COST;
            case "WASTE_THERMAL" -> WasteThermalUpgrade.COST;
            default -> { 
                lastError = "Unknown upgrade type: " + upgradeType;
                System.out.println("[UPGRADE] Unknown upgrade type: " + upgradeType); 
                yield -1; 
            }
        };
        if (cost < 0) return false;

        if (city.getState().getBudget() < cost) {
            lastError = "Insufficient budget to apply upgrade! Cost: " + cost + "$";
            System.out.println("[UPGRADE] Insufficient budget. Cost: " + cost);
            return false;
        }

        // Restrizione: l'upgrade termico dei rifiuti può essere applicato solo a un Waste Center
        if ("WASTE_THERMAL".equals(upgradeType) && base.getType() != StructureType.WASTE_CENTER) {
            lastError = "Waste Thermal Upgrade can only be applied to a Waste Center.";
            return false;
        }

        Structure upgraded = switch (upgradeType) {
            case "SEISMIC"       -> new SeismicUpgrade(base);
            case "WASTE_THERMAL" -> new WasteThermalUpgrade(base);
            default -> null;
        };
        if (upgraded == null) return false;

        city.getState().setBudget(city.getState().getBudget() - cost);
        city.removeDisasterObserver(base);
        // Sostituisce la struttura nella cella con la versione decorata
        cell.setStructure(upgraded);
        city.addDisasterObserver(upgraded);
        System.out.println("[UPGRADE] Applied " + upgradeType + " at (" + x + "," + y + ") | Cost: " + cost);
        city.notifyObserversPublic();
        return true;
    }

    /**
     * Salva lo stato corrente della città su file JSON con nome autogenerato.
     *
     * @param tick il numero del tick corrente, incluso nel file di salvataggio
     * @return il Path del file creato
     * @throws IOException se la scrittura su disco fallisce
     */
    public Path saveManualGame(int tick) throws IOException {
        return ioManager.saveManual(city, tick);
    }

    /**
     * Esegue un salvataggio automatico che sovrascrive il precedente della sessione corrente.
     *
     * @param tick il numero del tick corrente, incluso nel file di salvataggio
     * @return il Path del file creato
     * @throws IOException se la scrittura su disco fallisce
     */
    public Path autosaveGame(int tick) throws IOException {
        return ioManager.saveAuto(city, tick, gameSessionId);
    }

    /**
     * Restituisce la lista dei file di salvataggio disponibili, ordinati per data.
     *
     * @return lista di Path dei file JSON nella cartella saves/
     * @throws IOException se la lettura della cartella fallisce
     */
    public List<Path> listSaves() throws IOException {
        return ioManager.listSaves();
    }

    /**
     * Carica un salvataggio dal file indicato, ripristinando griglia e metriche.
     * Notifica gli osservatori dopo il caricamento per aggiornare la UI.
     *
     * @param path il Path del file JSON da caricare
     * @return il numero di tick salvato nel file
     * @throws IOException se la lettura del file fallisce
     */
    public int loadGame(Path path) throws IOException {
        int tick = ioManager.load(city, path);
        city.updateRoadConnections(); // Aggiorna le connessioni delle strade dopo il caricamento
        this.gameSessionId = String.valueOf(System.currentTimeMillis());
        city.notifyObserversPublic();
        return tick;
    }

    /**
     * Calcola il rimborso netto stimato per la demolizione di un edificio.
     * @return il netto (rimborso - costo) oppure 0 se non demolibile.
     */
    public int getEstimatedDemolitionRefund(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell != null && !cell.isEmpty() && cell.getStructure() instanceof Structure s) {
            int demolitionCost = s.getConstructionCost() / 10;
            int refund = s.getConstructionCost() / 2;
            return refund - demolitionCost;
        }
        return 0;
    }

    /**
     * Calcola il costo stimato per riparare un edificio danneggiato.
     * @return il costo oppure 0 se l'edificio è integro o distrutto.
     */
    public int getEstimatedRepairCost(int x, int y) {
        Cell cell = city.getGrid().getCell(x, y);
        if (cell != null && !cell.isEmpty() && cell.getStructure() instanceof Structure s) {
            if (!s.isDestroyed() && s.getHp() < s.getMaxHp()) {
                return (s.getMaxHp() - s.getHp()) / 2;
            }
        }
        return 0;
    }

    /** Restituisce lo stato corrente della città. */
    public CityState getState() { return city.getState(); }

    /** Restituisce la griglia della città. */
    public Grid getGrid() { return city.getGrid(); }

    /** Restituisce la rete elettrica corrente. */
    public PowerNetwork getPowerNet() { return city.getPowerNet(); }

    /** Restituisce l'ultimo messaggio d'errore di validazione registrato. */
    public String getLastError() { return lastError; }
}
