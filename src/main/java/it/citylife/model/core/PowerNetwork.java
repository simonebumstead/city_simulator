package it.citylife.model.core;

import it.citylife.model.structures.PowerPlant;

/**
 * Traccia la produzione e il consumo energetico della città nel tick corrente.
 *
 * Non gestisce la copertura geografica (quella è calcolata da City.isPowered()
 * con la distanza di Chebyshev): si limita a sommare la produzione totale
 * delle PowerPlant e il consumo totale degli edifici attivi, fornendo
 * un indicatore globale di bilancio energetico.
 *
 * I valori vengono azzerati all'inizio di ogni tick tramite reset(), in modo
 * che ogni tick parta da zero e accumuli i contributi freschi degli edifici.
 *
 * @see PowerPlant
 * @see City#updateState()
 * @see GameController#advanceTick()
 */
public class PowerNetwork {

    // Energia totale prodotta dalle PowerPlant nel tick corrente
    private int totalProduction;

    // Energia totale consumata dagli edifici attivi nel tick corrente
    private int totalConsumption;

    /**
     * Inizializza la rete con produzione e consumo a zero.
     */
    public PowerNetwork() {
        this.totalProduction  = 0;
        this.totalConsumption = 0;
    }

    /**
     * Aggiunge energia prodotta alla rete (chiamato da PowerPlant.applyEffects).
     *
     * @param amount unità di energia prodotte da aggiungere al totale
     */
    public void addProduction(int amount) {
        this.totalProduction += amount;
    }

    /**
     * Aggiunge carico alla rete (chiamato dagli edifici che consumano energia).
     *
     * @param amount unità di energia consumate da aggiungere al totale
     */
    public void addConsumption(int amount) {
        this.totalConsumption += amount;
    }

    /**
     * Verifica se la produzione totale copre il consumo totale nel tick corrente.
     *
     * @return true se non c'è blackout (produzione >= consumo), false altrimenti
     */
    public boolean hasEnoughPower() {
        return totalProduction >= totalConsumption;
    }

    /**
     * Azzera produzione e consumo per il tick successivo.
     * Chiamato da City.updateState() all'inizio di ogni tick.
     */
    public void reset() {
        this.totalProduction  = 0;
        this.totalConsumption = 0;
    }

    /** Restituisce l'energia totale prodotta nel tick corrente. */
    public int getTotalProduction()  { return totalProduction; }

    /** Restituisce l'energia totale consumata nel tick corrente. */
    public int getTotalConsumption() { return totalConsumption; }
}
