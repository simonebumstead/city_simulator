package it.citylife.ui;

import it.citylife.model.GameController;
import it.citylife.model.PolicyStrategy;
import it.citylife.model.StateObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Facade leggero tra il layer UI e {@link GameController}.
 *
 * Espone alla UI (DashboardView) un'interfaccia semplificata che maschera
 * i dettagli interni del modello: la UI non dipende mai direttamente da
 * GameController, mantenendo un disaccoppiamento pulito tra i due layer.
 *
 * Ogni metodo delega senza logica aggiuntiva al GameController sottostante;
 * non contiene regole di dominio né stato proprio (eccetto il riferimento
 * al controller).
 *
 * @see GameController
 * @see it.citylife.ui.DashboardView
 */
public class SimulationController {

    // Il controller di dominio a cui vengono delegate tutte le operazioni
    private final GameController controller;

    /**
     * Crea un SimulationController inizializzando un nuovo GameController (e quindi una nuova città).
     */
    public SimulationController() {
        this.controller = new GameController();
    }

    /**
     * Avanza la simulazione di un tick.
     * Delega a {@link GameController#advanceTick()}.
     */
    public void tick() {
        controller.advanceTick();
    }

    /**
     * Cambia la politica economica attiva.
     * Delega a {@link GameController#changePolicy(PolicyStrategy)}.
     *
     * @param policy la nuova politica da applicare (null ripristina la DefaultPolicy)
     */
    public void setPolicy(PolicyStrategy policy) {
        controller.changePolicy(policy);
    }

    /**
     * Registra un osservatore da notificare al termine di ogni tick.
     * Delega a {@link GameController#addObserver(StateObserver)}.
     *
     * @param observer l'osservatore da registrare (tipicamente DashboardView)
     */
    public void addObserver(StateObserver observer) {
        controller.addObserver(observer);
    }

    /**
     * Verifica se la rete elettrica della città è in equilibrio (produzione >= consumo).
     *
     * @return true se non c'è blackout, false altrimenti
     */
    public boolean hasPower() {
        return controller.getPowerNet().hasEnoughPower();
    }

    /**
     * Piazza una nuova struttura del tipo indicato nella cella (x, y).
     * Delega a {@link GameController#placeBuilding(String, int, int)}.
     *
     * @param type stringa del tipo di struttura (es. "RESIDENTIAL", "POWER_PLANT")
     * @param x    colonna della cella target
     * @param y    riga della cella target
     * @return true se il piazzamento è avvenuto con successo
     */
    public boolean placeBuilding(String type, int x, int y) {
        return controller.placeBuilding(type, x, y);
    }

    /**
     * Demolisce la struttura nella cella (x, y).
     * Delega a {@link GameController#demolish(int, int)}.
     *
     * @param x colonna della cella target
     * @param y riga della cella target
     * @return true se la demolizione è avvenuta con successo
     */
    public boolean demolish(int x, int y) {
        return controller.demolish(x, y);
    }

    /**
     * Ripara la struttura nella cella (x, y).
     * Delega a {@link GameController#repair(int, int)}.
     *
     * @param x colonna della cella target
     * @param y riga della cella target
     * @return true se la riparazione è avvenuta con successo
     */
    public boolean repair(int x, int y) {
        return controller.repair(x, y);
    }

    /**
     * Applica un upgrade alla struttura nella cella (x, y).
     * Delega a {@link GameController#upgradeBuilding(int, int, String)}.
     *
     * @param x           colonna della cella target
     * @param y           riga della cella target
     * @param upgradeType stringa del tipo di upgrade ("SEISMIC" o "WASTE_THERMAL")
     * @return true se l'upgrade è stato applicato con successo
     */
    public boolean upgrade(int x, int y, String upgradeType) {
        return controller.upgradeBuilding(x, y, upgradeType);
    }

    /**
     * Restituisce la griglia della città.
     *
     * @return la griglia 20×20 corrente
     */
    public it.citylife.model.Grid getGrid() {
        return controller.getGrid();
    }

    /**
     * Restituisce lo stato corrente della città.
     *
     * @return il CityState con tutte le metriche aggiornate
     */
    public it.citylife.model.CityState getState() {
        return controller.getState();
    }

    /**
     * Salva lo stato corrente della città su file JSON.
     * Delega a {@link GameController#saveGame(int)}.
     *
     * @param tick il numero di tick corrente da includere nel salvataggio
     * @return il Path del file JSON creato
     * @throws IOException se la scrittura su disco fallisce
     */
    public Path save(int tick) throws IOException {
        return controller.saveGame(tick);
    }

    /**
     * Restituisce la lista dei file di salvataggio disponibili.
     * Delega a {@link GameController#listSaves()}.
     *
     * @return lista di Path dei file JSON nella cartella saves/
     * @throws IOException se la lettura della cartella fallisce
     */
    public List<Path> listSaves() throws IOException {
        return controller.listSaves();
    }

    /**
     * Carica un salvataggio dal file indicato.
     * Delega a {@link GameController#loadGame(Path)}.
     *
     * @param path il Path del file JSON da caricare
     * @return il numero di tick registrato nel salvataggio
     * @throws IOException se la lettura del file fallisce
     */
    public int load(Path path) throws IOException {
        return controller.loadGame(path);
    }
}
